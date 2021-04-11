/*
 * Symbol recovery from elf file
 * =============================
 *
 * Copyright (C) 2017, 2019  Dave Marples  <dave@marples.net>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the names Orbtrace, Orbuculum nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include <stdlib.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <fcntl.h>
#include <ctype.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>

#ifdef OSX
    #include "osxelf.h"
#else
    #include <elf.h>
#endif

#include <stdint.h>
#include <assert.h>
#include "generics.h"
#include "symbols.h"

#define TEXT_SEGMENT ".text"

#define ELF_RELOAD_DELAY_TIME 1000000   /* Time before elf reload will be attempted when its been lost */
#define ELF_CHECK_DELAY_TIME  100000    /* Time that elf file has to be stable before it's considered complete */

// ====================================================================================================
// ====================================================================================================
// ====================================================================================================
// Internal Routines
// ====================================================================================================
// ====================================================================================================
// ====================================================================================================
bool _symbolsLoad( struct SymbolSet *s )

/* Load symbols from bfd library compatible file */

{
    uint32_t storage;
    bool dynamic = false;
    char **matching;

    bfd_init();

    /* Get information about the file being used */
    stat ( s->elfFile, &( s->st ) );
    s->abfd = bfd_openr( s->elfFile, NULL );

    if ( !s->abfd )
    {
        genericsReport( V_ERROR, "Couldn't open ELF file" EOL );
        return false;
    }

    s->abfd->flags |= BFD_DECOMPRESS;

    if ( bfd_check_format( s->abfd, bfd_archive ) )
    {
        genericsReport( V_ERROR, "Cannot get addresses from archive %s" EOL, s->elfFile );
        return false;
    }

    if ( ! bfd_check_format_matches ( s->abfd, bfd_object, &matching ) )
    {
        genericsReport( V_ERROR, "Ambigious format for file" EOL );
        return false;
    }

    if ( ( bfd_get_file_flags ( s->abfd ) & HAS_SYMS ) == 0 )
    {
        genericsReport( V_ERROR, "No symbols found" EOL );
        return false;
    }

    storage = bfd_get_symtab_upper_bound ( s->abfd ); /* This is returned in bytes */

    if ( storage == 0 )
    {
        storage = bfd_get_dynamic_symtab_upper_bound ( s->abfd );
        dynamic = true;
    }

    s->syms = ( asymbol ** )malloc( storage );

    if ( dynamic )
    {
        s->symcount = bfd_canonicalize_dynamic_symtab ( s->abfd, s->syms );
    }
    else
    {
        s->symcount = bfd_canonicalize_symtab ( s->abfd, s->syms );
    }

    return true;
}
// ====================================================================================================
// ====================================================================================================
// I'm not a fan of globals used for this kind of thing, but this is a library so we don't get a
// say in the matter. Lets at least hide what is going on in one place...access via _find_symbol.

static bool _found;
static uint32_t _searchaddr;
static const char **_function;
static const char **_filename;
static uint32_t *_line;
static asymbol **_syms;

static void _find_in_section( bfd *abfd, asection *section, void *data )

{
    /* If we already found it then return */
    if ( _found )
    {
        return;
    }

    /* If this section isn't memory-resident, then don't look further, otherwise get section base and size */
    /* (Ifdef is a work around for changes in binutils 2.34.                                               */
#ifdef bfd_get_section_vma

    if ( !( ( bfd_get_section_flags( abfd, section ) & SEC_ALLOC ) ) )
    {
        return;
    }

    bfd_vma vma = bfd_get_section_vma( abfd, section );
    bfd_size_type size = bfd_section_size( abfd, section );
#else

    if ( !( ( bfd_section_flags( section ) & SEC_ALLOC ) ) )
    {
        return;
    }

    bfd_vma vma = bfd_section_vma( section );
    bfd_size_type size = bfd_section_size( section );
#endif


    /* If address falls outside this section then don't look further */
    if ( ( _searchaddr < vma ) || ( _searchaddr > vma + size ) )
    {
        return;
    }

    _found = bfd_find_nearest_line( abfd, section, _syms, _searchaddr - vma, _filename, _function, _line );
}

static bool _find_symbol( struct SymbolSet *s, uint32_t workingAddr, const char **pfilename, const char **pfunction, uint32_t *pline )

{
    _syms = s->syms;
    _searchaddr = workingAddr;
    _filename = pfilename;
    _function = pfunction;
    _line = pline;
    _found = false;

    bfd_map_over_sections( s->abfd, _find_in_section, NULL );
    return _found;
}
// ====================================================================================================
// ====================================================================================================
bool SymbolLookup( struct SymbolSet *s, uint32_t addr, struct nameEntry *n, char *deleteMaterial )

/* Lookup function for address to line, and hence to function */

{
    const char *function = NULL;
    const char *filename = NULL;
    uint32_t line;

    assert( s );

    if ( ( addr & EXC_RETURN_MASK ) == EXC_RETURN )
    {
        /* Address is some sort of interrupt - see */
        n->filename = "";
        n->line = 0;

        switch ( addr & INT_ORIGIN_MASK )
        {
            case INT_ORIGIN_HANDLER:
                n->addr = INTERRUPT_HANDLER;
                n->function = "INT_FROM_HANDLER";
                break;

            case INT_ORIGIN_MAIN_STACK:
                n->addr = INTERRUPT_MAIN;
                n->function = "INT_FROM_MAIN_STACK";
                break;

            case INT_ORIGIN_PROC_STACK:
                n->addr = INTERRUPT_PROC;
                n->function = "INT_FROM_PROC_STACK";
                break;

            default:
                n->addr = INTERRUPT_UNKNOWN;
                n->function = "INT_FROM_UNKNOWN";
                break;

        }

        return false;
    }

    if ( _find_symbol( s, addr, &filename, &function, &line ) )
    {

        /* Remove any frontmatter off filename string that matches */
        if ( ( deleteMaterial ) && ( filename ) )
        {
            char *m = deleteMaterial;

            while ( ( *m ) && ( *filename ) && ( *filename == *m ) )
            {
                m++;
                filename++;
            }
        }

        n->filename = filename ? filename : "";
        n->function = function ? function : "";
        n->addr = addr;
        n->line = line;
        return true;
    }


    n->filename = "Unknown";
    n->function = "Unknown";
    n->addr = NOT_FOUND;
    n->line = 0;
    return false;
}
// ====================================================================================================
struct SymbolSet *SymbolSetCreate( char *filename )

{
    struct stat statbuf, newstatbuf;
    struct SymbolSet *s = ( struct SymbolSet * )calloc( sizeof( struct SymbolSet ), 1 );
    s->elfFile = strdup( filename );

    /* Make sure this file is stable before trying to load it */
    if ( stat( filename, &statbuf ) == 0 )
    {
        /* There is at least a file here */
        while ( 1 )
        {
            usleep( ELF_CHECK_DELAY_TIME );

            if ( stat( filename, &newstatbuf ) != 0 )
            {
                printf( "NO FILE!!!\n" );
                break;
            }

            /* We check filesize, modification time and status change time for any differences */
            if (
                        ( memcmp( &statbuf.st_size, &newstatbuf.st_size, sizeof( off_t ) ) ) ||
#ifdef OSX
                        ( memcmp( &statbuf.st_mtimespec, &newstatbuf.st_mtimespec, sizeof( struct timespec ) ) ) ||
                        ( memcmp( &statbuf.st_ctimespec, &newstatbuf.st_ctimespec, sizeof( struct timespec ) ) )
#else
                        ( memcmp( &statbuf.st_mtim, &newstatbuf.st_mtim, sizeof( struct timespec ) ) ) ||
                        ( memcmp( &statbuf.st_ctim, &newstatbuf.st_ctim, sizeof( struct timespec ) ) )
#endif
            )
            {
                /* Make this the version we check next time around */
                memcpy( &statbuf, &newstatbuf, sizeof( struct stat ) );
                continue;
            }

            if ( _symbolsLoad( s ) )
            {
                return s;
            }
            else
            {
                break;
            }
        }
    }

    /* If we reach here we weren't successful, so delete the allocated memory */
    free( s->elfFile );
    free( s );
    s = NULL;

    return s;
}
// ====================================================================================================
void SymbolSetDelete( struct SymbolSet **s )

{
    if ( ( *s ) && ( ( *s )->abfd ) )
    {
        bfd_close( ( *s )->abfd );
        free( ( *s )->elfFile );
        free( *s );
        *s = NULL;
    }
}
// ====================================================================================================
bool SymbolSetValid( struct SymbolSet **s, char *filename )

{
    struct stat n;

    if ( 0 != stat( filename, &n ) )
    {
        /* We can't even stat the file, assume it's invalid */
        SymbolSetDelete( s );
        return false;
    }

    /* We check filesize, modification time and status change time for any differences */
    if ( ( !( *s ) ) ||
            ( memcmp( &n.st_size, &( ( *s )->st.st_size ), sizeof( off_t ) ) ) ||

#ifdef OSX
            ( memcmp( &n.st_mtimespec, &( ( *s )->st.st_mtimespec ), sizeof( struct timespec ) ) ) ||
            ( memcmp( &n.st_ctimespec, &( ( *s )->st.st_ctimespec ), sizeof( struct timespec ) ) )
#else
            ( memcmp( &n.st_mtim, &( ( *s )->st.st_mtim ), sizeof( struct timespec ) ) ) ||
            ( memcmp( &n.st_ctim, &( ( *s )->st.st_ctim ), sizeof( struct timespec ) ) )
#endif
       )
    {
        SymbolSetDelete( s );
        return false;
    }
    else
    {
        return true;
    }
}
// ====================================================================================================
bool SymbolSetLoad( struct SymbolSet **s, char *filename )

{
    assert( *s == NULL );

    *s = SymbolSetCreate( filename );
    return ( ( *s ) != NULL );
}
// ====================================================================================================

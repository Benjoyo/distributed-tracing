/*
 * Network Server support
 * ======================
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
#include <unistd.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <assert.h>
#include <strings.h>
#include "generics.h"
#include "nwclient.h"


#define CLIENT_TERM_INTERVAL_US (10000)       /* Interval to check for all clients lost */

/* Master structure for the nwclients */
struct nwclientsHandle

{
    struct nwClient *firstClient;             /* Head of linked list of network clients */
    pthread_mutex_t clientList;               /* Lock for list of network clients */

    int sockfd;                               /* The socket for the inferior */
    pthread_t ipThread;                       /* The listening thread for n/w clients */
    bool finish;                              /* Its time to leave */
};

/* List of any connected network clients */
struct nwClient

{
    int handle;                               /* Handle to client */
    pthread_t thread;                         /* Execution thread */
    struct nwclientsHandle *parent;           /* Who owns this list */
    struct nwClient *nextClient;
    struct nwClient *prevClient;
    bool finish;                              /* Flag indicating it's time to cease operation */

    /* Parameters used to run the client */
    int portNo;                               /* Port of connection */
    int listenHandle;                         /* Handle for listener */

};

static int lock_with_timeout( pthread_mutex_t *mutex, const struct timespec *ts )
{
    int ret;
    int left, step;

    left = ts->tv_sec * 1000;       /* how much waiting is left, in msec */
    step = 10;              /* msec to sleep at each trywait() failure */

    do
    {
        if ( ( ret = pthread_mutex_trylock( mutex ) ) != 0 )
        {
            struct timespec dly;

            dly.tv_sec = 0;
            dly.tv_nsec = step * 1000000;
            nanosleep( &dly, NULL );

            left -= step;
        }
    }
    while ( ret != 0 && left > 0 );

    return ret;
}

// ====================================================================================================
// Network server implementation for raw SWO feed
// ====================================================================================================
static void _clientRemove( struct nwClient *c )

{
    const struct timespec ts = {.tv_sec = 1, .tv_nsec = 0};

    close( c->portNo );
    close( c->listenHandle );

    /* First of all, make sure we can get access to the client list */

    if ( lock_with_timeout( &c->parent->clientList, &ts ) < 0 )
    {
        genericsExit( -1, "Failed to acquire mutex" EOL );
    }

    if ( c->prevClient )
    {
        c->prevClient->nextClient = c->nextClient;
    }
    else
    {
        c->parent->firstClient = c->nextClient;
    }

    if ( c->nextClient )
    {
        c->nextClient->prevClient = c->prevClient;
    }

    /* OK, we made our modifications */
    pthread_mutex_unlock( &c->parent->clientList );

    /* Remove the memory that was allocated for this client */
    free( c );
}
// ====================================================================================================
static void *_client( void *args )

/* Handle an individual network client account */

{
    struct nwClient *c = ( struct nwClient * )args;
    int readDataLen;
    uint8_t maxTransitPacket[TRANSFER_SIZE];

    while ( !c->finish )
    {
        readDataLen = read( c->listenHandle, maxTransitPacket, TRANSFER_SIZE );

        if ( ( c->finish ) || ( readDataLen <= 0 ) || ( write( c->portNo, maxTransitPacket, readDataLen ) < 0 ) )
        {
            /* This port went away, so remove it */
            genericsReport( V_INFO, "Connection dropped" EOL );
            c->finish = true;
        }
    }

    close( c->listenHandle );

    _clientRemove( c );
    return NULL;
}
// ====================================================================================================
static void *_listenTask( void *arg )

{
    struct nwclientsHandle *h = ( struct nwclientsHandle * )arg;
    const struct timespec ts = {.tv_sec = 1, .tv_nsec = 0};
    int newsockfd;
    socklen_t clilen;
    struct sockaddr_in cli_addr;
    int f[2];                               /* File descriptor set for pipe */
    struct nwClient *client;
    char s[100];

    clilen = sizeof( cli_addr );
    listen( h->sockfd, 5 );

    while ( !h->finish )
    {
        newsockfd = accept( h->sockfd, ( struct sockaddr * ) &cli_addr, &clilen );

        if ( h->finish )
        {
            close( newsockfd );
            break;
        }

        inet_ntop( AF_INET, &cli_addr.sin_addr, s, 99 );
        genericsReport( V_INFO, "New connection from %s" EOL, s );

        /* We got a new connection - spawn a thread to handle it */
        if ( !pipe( f ) )
        {
            client = ( struct nwClient * )calloc( 1, sizeof( struct nwClient ) );
            client->handle = f[1];
            client->parent = h;
            client->listenHandle = f[0];
            client->portNo = newsockfd;

            if ( !pthread_create( &( client->thread ), NULL, &_client, client ) )
            {
                /* Auto-cleanup for this thread */
                pthread_detach( client->thread );

                /* Hook into linked list */
                if ( lock_with_timeout( &h->clientList, &ts ) < 0 )
                {
                    genericsExit( -1, "Failed to acquire mutex" EOL );
                }

                client->nextClient = h->firstClient;
                client->prevClient = NULL;

                if ( client->nextClient )
                {
                    client->nextClient->prevClient = client;
                }

                h->firstClient = client;

                pthread_mutex_unlock( &h->clientList );
            }
        }
    }

    close( h->sockfd );
    return NULL;
}
// ====================================================================================================
// ====================================================================================================
// ====================================================================================================
// Externally available routines
// ====================================================================================================
// ====================================================================================================
// ====================================================================================================
void nwclientSend( struct nwclientsHandle *h, uint32_t len, uint8_t *buffer )

{
    assert( h );
    assert( len );

    const struct timespec ts = {.tv_sec = 1, .tv_nsec = 0};
    struct nwClient *n = h->firstClient;

    if ( !h->finish )
    {
        if ( lock_with_timeout( &h->clientList, &ts ) < 0 )
        {
            genericsExit( -1, "Failed to acquire mutex" EOL );
        }

        while ( n )
        {
            write( n->handle, buffer, len );
            n = n->nextClient;
        }

        pthread_mutex_unlock( &h->clientList );
    }
}
// ====================================================================================================
struct nwclientsHandle *nwclientStart( int port )

/* Creating the listening server thread */

{
    struct sockaddr_in serv_addr;
    int flag = 1;
    struct nwclientsHandle *h = ( struct nwclientsHandle * )calloc( 1, sizeof( struct nwclientsHandle ) );

    if ( !h )
    {
        return NULL;
    }

    h->sockfd = socket( AF_INET, SOCK_STREAM, 0 );
    setsockopt( h->sockfd, SOL_SOCKET, SO_REUSEPORT, &flag, sizeof( flag ) );

    if ( h->sockfd < 0 )
    {
        genericsReport( V_ERROR, "Error opening socket" EOL );
        goto free_and_return;
    }

    bzero( ( char * ) &serv_addr, sizeof( serv_addr ) );
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons( port );

    if ( setsockopt( h->sockfd, SOL_SOCKET, SO_REUSEADDR, &( int )
{
    1
}, sizeof( int ) ) < 0 )
    {
        genericsReport( V_ERROR, "setsockopt(SO_REUSEADDR) failed" );
        goto free_and_return;
    }

    if ( bind( h->sockfd, ( struct sockaddr * ) &serv_addr, sizeof( serv_addr ) ) < 0 )
    {
        genericsReport( V_ERROR, "Error on binding" EOL );
        goto free_and_return;
    }

    /* Create a mutex to lock the client list */
    pthread_mutex_init( &h->clientList, NULL );

    /* We have the listening socket - spawn a thread to handle it */
    if ( pthread_create( &( h->ipThread ), NULL, &_listenTask, h ) )
    {
        genericsReport( V_ERROR, "Failed to create listening thread" EOL );
        goto free_and_return;
    }

    return h;

free_and_return:
    free( h );
    return NULL;
}
// ====================================================================================================
void nwclientShutdown( struct nwclientsHandle *h )

{
    const struct timespec ts = {.tv_sec = 1, .tv_nsec = 0};

    if ( !h )
    {
        return;
    }

    struct nwClient *c = h->firstClient;

    /* Flag that we're ending */
    h->finish = true;

    if ( lock_with_timeout( &h->clientList, &ts ) < 0 )
    {
        genericsExit( -1, "Failed to acquire mutex" EOL );
    }

    /* Tell all the clients to terminate */
    while ( c )
    {
        c->finish = true;

        /* Closing both ends of the connection will kill the client */
        close( c->handle );
        close( c->listenHandle );

        /* This is safe because we are locked by the mutex */
        c = c->nextClient;
    }

    pthread_mutex_unlock( &h->clientList );
}
// ====================================================================================================
bool nwclientShutdownComplete( struct nwclientsHandle *h )

{
    if ( ! h->firstClient )
    {
        free( h );
        return true;
    }

    return false;
}
// ====================================================================================================

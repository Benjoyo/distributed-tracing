/*
 * Msg Sequencer Module
 * ====================
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

/*
 * Sequencer for re-ordering decoded messages from the ITM according to timestamp
 * information in the flow.
 *
 * from https://static.docs.arm.com/ddi0403/e/DDI0403E_B_armv7m_arm.pdf
 */

#ifndef _MSGSEQ_H_
#define _MSGSEQ_H_

#include <stdbool.h>
#include <stdint.h>

#ifdef DEBUG
    #include "generics.h"
#else
    #define genericsReport(x...)
#endif

#include "itmDecoder.h"
#include "msgDecoder.h"

#ifdef __cplusplus
extern "C" {
#endif

struct MsgSeq

{
    struct ITMDecoder *i;

    uint32_t wp;
    uint32_t rp;
    uint32_t pbl;
    struct msg *pbuffer;
    bool releaseTimeMsg;
};

// ====================================================================================================

void MsgSeqInit( struct MsgSeq *d, struct ITMDecoder *i, uint32_t maxEntries );
struct msg *MsgSeqGetPacket( struct MsgSeq *d );

bool MsgSeqPump( struct MsgSeq *d, uint8_t c );

// ====================================================================================================
#ifdef __cplusplus
}
#endif
#endif

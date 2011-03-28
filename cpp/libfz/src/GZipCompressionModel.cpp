/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

#include "GZipCompressionModel.h"
#include <zlib.h>

using namespace std;

namespace femtozip {

GZipCompressionModel::GZipCompressionModel() {
}

GZipCompressionModel::~GZipCompressionModel() {
}


void GZipCompressionModel::build(DocumentList& documents) {
}

void GZipCompressionModel::encodeLiteral(int aByte, void *context) {
    throw "GZipCompressionModel::encodeLiteral unsupported";
}

void GZipCompressionModel::encodeSubstring(int offset, int length, void *context) {
    throw "GZipCompressionModel::encodeSubstring unsupported";
}

void GZipCompressionModel::endEncoding(void *context) {
    throw "GZipCompressionModel::endEncoding unsupported";
}

bool GZipCompressionModel::useDict() {
    return false;
}

static const int CHUNK = 16*1024;

// Hacked from zpipe.c
void GZipCompressionModel::compress(const char *buf, int length, ostream& out) {
    unsigned have;
    int ret;
    z_stream strm;
    unsigned char outBuf[CHUNK];

    /* allocate deflate state */
    strm.zalloc = Z_NULL;
    strm.zfree = Z_NULL;
    strm.opaque = Z_NULL;
    ret = deflateInit(&strm, Z_BEST_COMPRESSION);
    if (ret != Z_OK) {
        throw "deflateInit failed";
    }

    /* compress until end of file */
    strm.avail_in = length;
    strm.next_in = (unsigned char *)buf;

    if (useDict() && dict && dictLen > 0) {
        deflateSetDictionary(&strm, (unsigned char *)dict, dictLen);
    }

    /* run deflate() on input until output buffer not full, finish
       compression if all of source has been read in */
    do {
        strm.avail_out = CHUNK;
        strm.next_out = outBuf;
        ret = deflate(&strm, Z_FINISH);    /* no bad return value */
        //assert(ret != Z_STREAM_ERROR);  /* state not clobbered */
        have = CHUNK - strm.avail_out;
        out.write((char *)outBuf, have);
    } while (strm.avail_out == 0);
    //assert(strm.avail_in == 0);     /* all input will be used */

    //assert(ret == Z_STREAM_END);        /* stream will be complete */

    /* clean up and return */
    deflateEnd(&strm);
}

void GZipCompressionModel::decompress(const char *buf, int length, ostream& out) {
    int ret;
    unsigned have;
    z_stream strm;
    unsigned char outBuf[CHUNK];

    /* allocate inflate state */
    strm.zalloc = Z_NULL;
    strm.zfree = Z_NULL;
    strm.opaque = Z_NULL;
    strm.avail_in = 0;
    strm.next_in = Z_NULL;
    ret = inflateInit(&strm);
    if (ret != Z_OK) {
        throw "inflateInit failed";
    }

    /* decompress until deflate stream ends or end of file */
    strm.avail_in = length;
    strm.next_in = (unsigned char *)buf;

    /* run inflate() on input until output buffer not full */
    do {
        strm.avail_out = CHUNK;
        strm.next_out = outBuf;
        ret = inflate(&strm, Z_NO_FLUSH);
        //assert(ret != Z_STREAM_ERROR);  /* state not clobbered */
        switch (ret) {
        case Z_NEED_DICT:
            if (useDict() && dict && dictLen > 0) {
                inflateSetDictionary(&strm, (unsigned char *)dict, dictLen);
                continue;
            }
            ret = Z_DATA_ERROR;     /* and fall through */
        case Z_DATA_ERROR:
        case Z_MEM_ERROR:
            (void)inflateEnd(&strm);
            return;
        }
        have = CHUNK - strm.avail_out;
        if (out.write((char *)outBuf, have)) {
            (void)inflateEnd(&strm);
            return;
        }
    } while (strm.avail_out == 0 || ret == Z_NEED_DICT);

    /* done when inflate() says it's done */

    /* clean up and return */
    (void)inflateEnd(&strm);
}

}

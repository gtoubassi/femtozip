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
/*
 * HuffmanDecoder.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef HUFFMANDECODER_H_
#define HUFFMANDECODER_H_

#include <istream>
#include "BitInput.h"
#include "Codeword.h"

namespace femtozip {

template <class T> class HuffmanDecoder {
protected:
    BitInput bitIn;
    T& model;
    long long bitBuf;
    int availableBits;
    bool endOfStream;

public:
    HuffmanDecoder(const char *buf, int length, T& model) : bitIn(buf, length), model(model) {
        bitBuf = 0;
        availableBits = 0;
        endOfStream = false;
    }

    inline int decodeSymbol() {
        if (endOfStream) {
            return -1;
        }

        while (availableBits < 32) {
            int bit = bitIn.readBit();
            if (bit == -1) {
                break;
            }
            if (bit) {
                bitBuf |= 1LL << availableBits;
            }
            availableBits++;
        }

        Codeword& decoded = model.decode((int)bitBuf);
        if (model.isEOF(decoded)) {
            endOfStream = true;
            return -1;
        }
        bitBuf >>= decoded.bitLength;
        availableBits -= decoded.bitLength;
        return decoded.symbol;
    }
};

}

#endif /* HUFFMANDECODER_H_ */

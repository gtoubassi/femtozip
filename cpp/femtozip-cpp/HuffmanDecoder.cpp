/*
 * HuffmanDecoder.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include "HuffmanDecoder.h"

namespace femtozip {

HuffmanDecoder::HuffmanDecoder(istream& input, HuffmanModel& model) : bitIn(input), model(model) {
    bitBuf = 0;
    availableBits = 0;
    endOfStream = false;
}

int HuffmanDecoder::decodeSymbol() {
    if (endOfStream) {
        return -1;
    }

    while (availableBits < 32) {
        int bit = bitIn.readBit();
        if (bit == -1) {
            break;
        }
        if (bit) {
            bitBuf |= 1L << availableBits;
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


}

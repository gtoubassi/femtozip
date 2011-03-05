/*
 * Codeword.h
 *
 *  Created on: Mar 3, 2011
 *      Author: gtoubassi
 */

#ifndef CODEWORD_H_
#define CODEWORD_H_

#include "BitOutput.h"

namespace femtozip {

class Codeword {
public:
    int value;
    int bitLength;
    int symbol;

    Codeword() :value(0), bitLength(0), symbol(-1) {};

    inline void appendBit(int v) {
        value |= (0x1 & v) << bitLength;
        bitLength++;
    }

    inline void write(BitOutput& bitOut) {
        int l = bitLength;
        int v = value;

        while (l > 0) {
            bitOut.writeBit(v & 0x1);
            l--;
            v >>= 1;
        }
    }

    bool operator==(const Codeword& o) const {
        return value == o.value && bitLength == o.bitLength && symbol == o.symbol;
    }
};

}

#endif /* CODEWORD_H_ */

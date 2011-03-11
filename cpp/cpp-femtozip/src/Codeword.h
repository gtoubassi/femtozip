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
 * Codeword.h
 *
 *  Created on: Mar 3, 2011
 *      Author: gtoubassi
 */

#ifndef CODEWORD_H_
#define CODEWORD_H_

#include "BitOutput.h"
#include "DataIO.h"

namespace femtozip {

class Codeword {
public:
    int value;
    int bitLength;
    int symbol;

    Codeword() :value(0), bitLength(0), symbol(-1) {};

    virtual void load(DataInput& in) {
        in >> value;
        in >> bitLength;
        in >> symbol;
    }

    virtual void save(DataOutput& out) {
        out << value << bitLength << symbol;
    }

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

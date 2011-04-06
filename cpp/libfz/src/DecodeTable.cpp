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
 * DecodeTable.cpp
 *
 *  Created on: Mar 3, 2011
 *      Author: gtoubassi
 */

#include "DecodeTable.h"

namespace femtozip {

DecodeTable::DecodeTable() : codes(256), tables(256, NULL) {
}

DecodeTable::~DecodeTable() {
    for (vector<DecodeTable *>::iterator i = tables.begin(); i < tables.end(); i++) {
        delete *i;
    }
}

void DecodeTable::build(vector<Codeword>& encoding, int consumedBitLength) {
    for (vector<Codeword>::iterator i = encoding.begin(); i != encoding.end(); i++) {
        if (i->symbol != -1) {
            buildCodeword(consumedBitLength, *i);
        }
    }
}

void DecodeTable::buildCodeword(int consumedBitLength, Codeword& code) {
    int activeBitLength = code.bitLength - consumedBitLength;

    if (activeBitLength <= 8) {

        for (int i = 0, count = 1 << (8 - activeBitLength); i < count; i++) {
            int index = (i << activeBitLength) | (code.value >> consumedBitLength);
            codes[index] = code;
        }
    }
    else {
        int index = (code.value >> consumedBitLength) & 0xff;
        DecodeTable *subtable = tables[index];
        if (!subtable) {
            subtable = new DecodeTable();
            tables[index] = subtable;
        }
        subtable->buildCodeword(consumedBitLength + 8, code);
    }
}

Codeword& DecodeTable::decode(int value) {
    int index = value & 0xff;
    Codeword& code = codes[index];
    if (code.bitLength != 0) {
        return code;
    }
    else {
        return tables[index]->decode(value>>8);
    }
}


}

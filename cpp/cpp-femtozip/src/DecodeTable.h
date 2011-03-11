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
 * DecodeTable.h
 *
 *  Created on: Mar 3, 2011
 *      Author: gtoubassi
 */

#ifndef DECODETABLE_H_
#define DECODETABLE_H_

#include <vector>
#include "Codeword.h"
#include "DataIO.h"

namespace femtozip {

class DecodeTable {

protected:
    void buildCodeword(int consumedBitLength, Codeword& code);

public:
    vector<Codeword> codes;
    vector<DecodeTable*> tables;

    DecodeTable();
    ~DecodeTable();

    virtual void load(DataInput& in);
    virtual void save(DataOutput& out);

    void build(vector<Codeword>& encoding, int consumedBitLength = 0);
    Codeword& decode(int value);
};

}

#endif /* DECODETABLE_H_ */

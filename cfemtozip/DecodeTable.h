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

namespace femtozip {

class DecodeTable {

protected:
    void buildCodeword(int consumedBitLength, Codeword& code);

public:
    vector<Codeword> codes;
    vector<DecodeTable*> tables;

    DecodeTable();
    ~DecodeTable();

    void build(vector<Codeword>& encoding, int consumedBitLength = 0);
    Codeword& decode(int value);
};

}

#endif /* DECODETABLE_H_ */

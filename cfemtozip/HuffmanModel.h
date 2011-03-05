/*
 * HuffmanModel.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef HUFFMANMODEL_H_
#define HUFFMANMODEL_H_

#include "Codeword.h"

namespace femtozip {

class HuffmanModel {
public:
    virtual ~HuffmanModel() {}
    virtual Codeword& getCodewordForEOF() = 0;
    virtual Codeword& encode(int symbol) = 0;
    virtual Codeword& decode(int bits) = 0;
    virtual bool isEOF(Codeword& codeword) = 0;
};

}

#endif /* HUFFMANMODEL_H_ */

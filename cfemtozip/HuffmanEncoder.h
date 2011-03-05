/*
 * HuffmanEncoder.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef HUFFMANENCODER_H_
#define HUFFMANENCODER_H_

#include <iostream>
#include "BitOutput.h"
#include "HuffmanModel.h"

namespace femtozip {

class HuffmanEncoder {
protected:
    BitOutput bitOut;
    HuffmanModel& model;

public:
    HuffmanEncoder(ostream& output, HuffmanModel& model);

    void encodeSymbol(int symbol);
    void finish();

};

}

#endif /* HUFFMANENCODER_H_ */

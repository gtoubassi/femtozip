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
#include "HuffmanModel.h"

namespace femtozip {

class HuffmanDecoder {
protected:
    BitInput bitIn;
    HuffmanModel& model;
    long bitBuf;
    int availableBits;
    bool endOfStream;

public:
    HuffmanDecoder(istream& input, HuffmanModel& model);

    int decodeSymbol();
};

}

#endif /* HUFFMANDECODER_H_ */

/*
 * HuffmanEncoder.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include "HuffmanEncoder.h"

namespace femtozip {

HuffmanEncoder::HuffmanEncoder(ostream& output, HuffmanModel& model) : bitOut(output), model(model) {
}

void HuffmanEncoder::encodeSymbol(int symbol) {
    model.encode(symbol).write(bitOut);
}

void HuffmanEncoder::finish() {
    model.getCodewordForEOF().write(bitOut); //EOF
    bitOut.flush();
}

}


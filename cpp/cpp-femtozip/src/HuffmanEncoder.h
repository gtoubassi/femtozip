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
 * HuffmanEncoder.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef HUFFMANENCODER_H_
#define HUFFMANENCODER_H_

#include <iostream>
#include "BitOutput.h"

namespace femtozip {

template <class T> class HuffmanEncoder {
protected:
    BitOutput bitOut;
    T& model;

public:
    HuffmanEncoder(ostream& output, T& model) : bitOut(output), model(model) {
    }

    inline void encodeSymbol(int symbol) {
        model.encode(symbol).write(bitOut);
    }

    inline void finish() {
        model.getCodewordForEOF().write(bitOut); //EOF
        bitOut.flush();
    }


};

}

#endif /* HUFFMANENCODER_H_ */

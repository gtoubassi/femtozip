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

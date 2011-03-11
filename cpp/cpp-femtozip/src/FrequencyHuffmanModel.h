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
 * FrequencyHuffmanModel.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef FREQUENCYHUFFMANMODEL_H_
#define FREQUENCYHUFFMANMODEL_H_

#include <vector>
#include "HuffmanModel.h"
#include "Codeword.h"
#include "DecodeTable.h"

namespace femtozip {

class FrequencyHuffmanModel : public HuffmanModel {

protected:
    vector<Codeword> encoding;
    DecodeTable decoding;

    void computeHuffmanCoding(vector<int>& histogram);

public:

    static void computeHistogramWithEOFSymbol(vector<int>& histogram, const char *data, int len);

    FrequencyHuffmanModel();
    FrequencyHuffmanModel(vector<int>& histogram, bool allSymbolsSampled);
    virtual ~FrequencyHuffmanModel();

    virtual void load(DataInput& in);
    virtual void save(DataOutput& out);

    virtual Codeword& getCodewordForEOF();
    virtual Codeword& encode(int symbol);
    virtual Codeword& decode(int bits);
    virtual bool isEOF(Codeword& codeword);
};

}

#endif /* FREQUENCYHUFFMANMODEL_H_ */

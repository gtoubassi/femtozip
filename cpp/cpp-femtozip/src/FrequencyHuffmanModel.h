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

    FrequencyHuffmanModel(vector<int>& histogram, bool allSymbolsSampled);
    virtual ~FrequencyHuffmanModel();

    virtual Codeword& getCodewordForEOF();
    virtual Codeword& encode(int symbol);
    virtual Codeword& decode(int bits);
    virtual bool isEOF(Codeword& codeword);
};

}

#endif /* FREQUENCYHUFFMANMODEL_H_ */

/*
 * OffsetNibbleHuffmanModel.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef OFFSETNIBBLEHUFFMANMODEL_H_
#define OFFSETNIBBLEHUFFMANMODEL_H_

#include "HuffmanModel.h"
#include "FrequencyHuffmanModel.h"

namespace femtozip {

class OffsetNibbleHuffmanModel: public femtozip::HuffmanModel {
protected:

    enum State {
        LiteralLengthState, OffsetNibble0State, OffsetNibble1State, OffsetNibble2State, OffsetNibble3State
    };

    FrequencyHuffmanModel *literalLengthModel;
    FrequencyHuffmanModel *offsetNibble0Model;
    FrequencyHuffmanModel *offsetNibble1Model;
    FrequencyHuffmanModel *offsetNibble2Model;
    FrequencyHuffmanModel *offsetNibble3Model;
    State state; // XXX Thread Safety!.

public:
    OffsetNibbleHuffmanModel(FrequencyHuffmanModel *literalLengthModel,
            FrequencyHuffmanModel *offsetNibble0Model,
            FrequencyHuffmanModel *offsetNibble1Model,
            FrequencyHuffmanModel *offsetNibble2Model,
            FrequencyHuffmanModel *offsetNibble3Model);

    virtual ~OffsetNibbleHuffmanModel();

    void reset();

    virtual Codeword& getCodewordForEOF();
    virtual Codeword& encode(int symbol);
    virtual Codeword& decode(int bits);
    virtual bool isEOF(Codeword& codeword);
};

}

#endif /* OFFSETNIBBLEHUFFMANMODEL_H_ */

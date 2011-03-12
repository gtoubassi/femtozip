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

class OffsetNibbleHuffmanModel  {
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
    OffsetNibbleHuffmanModel();
    OffsetNibbleHuffmanModel(FrequencyHuffmanModel *literalLengthModel,
            FrequencyHuffmanModel *offsetNibble0Model,
            FrequencyHuffmanModel *offsetNibble1Model,
            FrequencyHuffmanModel *offsetNibble2Model,
            FrequencyHuffmanModel *offsetNibble3Model);

    ~OffsetNibbleHuffmanModel();

    void load(DataInput& in);
    void save(DataOutput& out);

    void reset();

    inline Codeword& getCodewordForEOF() {
        return literalLengthModel->getCodewordForEOF();
    }

    inline Codeword& encode(int symbol) {
        switch (state) {
        case LiteralLengthState:
            if (symbol > 255) {
                state = OffsetNibble0State;
            }
            return literalLengthModel->encode(symbol);
        case OffsetNibble0State:
            state = OffsetNibble1State;
            return offsetNibble0Model->encode(symbol);
        case OffsetNibble1State:
            state = OffsetNibble2State;
            return offsetNibble1Model->encode(symbol);
        case OffsetNibble2State:
            state = OffsetNibble3State;
            return offsetNibble2Model->encode(symbol);
        case OffsetNibble3State:
            state = LiteralLengthState;
            return offsetNibble3Model->encode(symbol);
        default:
            throw "encode: illegal state";
        }
    }

    inline Codeword& decode(int bits) {
        switch (state) {
        case LiteralLengthState:{
                Codeword& codeword = literalLengthModel->decode(bits);
                if (codeword.symbol > 255) {
                    state = OffsetNibble0State;
                }
                return codeword;
            }
        case OffsetNibble0State:
            state = OffsetNibble1State;
            return offsetNibble0Model->decode(bits);
        case OffsetNibble1State:
            state = OffsetNibble2State;
            return offsetNibble1Model->decode(bits);
        case OffsetNibble2State:
            state = OffsetNibble3State;
            return offsetNibble2Model->decode(bits);
        case OffsetNibble3State:
            state = LiteralLengthState;
            return offsetNibble3Model->decode(bits);
        default:
            throw "encode: illegal state";
        }
    }

    inline bool isEOF(Codeword& codeword) {
        return state == OffsetNibble0State && getCodewordForEOF() == codeword;
    }
};

}

#endif /* OFFSETNIBBLEHUFFMANMODEL_H_ */

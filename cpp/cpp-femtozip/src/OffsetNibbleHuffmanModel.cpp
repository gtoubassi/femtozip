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
 * OffsetNibbleHuffmanModel.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include "OffsetNibbleHuffmanModel.h"

namespace femtozip {

OffsetNibbleHuffmanModel::OffsetNibbleHuffmanModel() {

}

OffsetNibbleHuffmanModel::OffsetNibbleHuffmanModel(FrequencyHuffmanModel *literalLengthModel,
        FrequencyHuffmanModel *offsetNibble0Model,
        FrequencyHuffmanModel *offsetNibble1Model,
        FrequencyHuffmanModel *offsetNibble2Model,
        FrequencyHuffmanModel *offsetNibble3Model) :

        literalLengthModel(literalLengthModel),
        offsetNibble0Model(offsetNibble0Model),
        offsetNibble1Model(offsetNibble1Model),
        offsetNibble2Model(offsetNibble2Model),
        offsetNibble3Model(offsetNibble3Model),
        state(LiteralLengthState)
{
}

OffsetNibbleHuffmanModel::~OffsetNibbleHuffmanModel() {
    delete literalLengthModel;
    delete offsetNibble0Model;
    delete offsetNibble1Model;
    delete offsetNibble2Model;
    delete offsetNibble3Model;
}

void OffsetNibbleHuffmanModel::load(DataInput& in) {
    literalLengthModel = new FrequencyHuffmanModel();
    offsetNibble0Model = new FrequencyHuffmanModel();
    offsetNibble1Model = new FrequencyHuffmanModel();
    offsetNibble2Model = new FrequencyHuffmanModel();
    offsetNibble3Model = new FrequencyHuffmanModel();

    literalLengthModel->load(in);
    offsetNibble0Model->load(in);
    offsetNibble1Model->load(in);
    offsetNibble2Model->load(in);
    offsetNibble3Model->load(in);
}

void OffsetNibbleHuffmanModel::save(DataOutput& out) {
    literalLengthModel->save(out);
    offsetNibble0Model->save(out);
    offsetNibble1Model->save(out);
    offsetNibble2Model->save(out);
    offsetNibble3Model->save(out);
}

void OffsetNibbleHuffmanModel::reset() {
    state = LiteralLengthState;
}


}

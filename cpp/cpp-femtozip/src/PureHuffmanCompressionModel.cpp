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
 * PureHuffmanModel.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include <streambuf>
#include "PureHuffmanCompressionModel.h"
#include "NoCopyReadOnlyStreamBuf.h"
#include "HuffmanEncoder.h"
#include "HuffmanDecoder.h"

using namespace std;

namespace femtozip {

PureHuffmanCompressionModel::PureHuffmanCompressionModel() : codeModel(0) {
}

PureHuffmanCompressionModel::~PureHuffmanCompressionModel() {
    if (codeModel) {
        delete codeModel;
    }
}

void PureHuffmanCompressionModel::load(DataInput& in) {
    CompressionModel::load(in);
    bool hasModel;
    in >> hasModel;
    if (hasModel) {
        codeModel = new FrequencyHuffmanModel();
        codeModel->load(in);
    }
}

void PureHuffmanCompressionModel::save(DataOutput& out) {
    CompressionModel::save(out);
    out << (codeModel ? true : false);
    if (codeModel) {
        codeModel->save(out);
    }
}

void PureHuffmanCompressionModel::build(DocumentList& documents) {
    vector<int> histogram(256, 0);
    for (int i = 0, count = documents.size(); i < count; i++) {
        int length;
        const char *bytes = documents.get(i, length);
        for (int j = 0; j < length; j++) {
            histogram[((int)bytes[j]) & 0xff]++;
        }
        histogram[histogram.size() - 1]++; // EOF
        documents.release(bytes);
    }

    codeModel = new FrequencyHuffmanModel(histogram, false);
}

void PureHuffmanCompressionModel::encodeLiteral(int aByte) { throw "PureHuffmanModel::encodeLiteral should not be invoked";}
void PureHuffmanCompressionModel::encodeSubstring(int offset, int length) { throw "PureHuffmanModel::encodeSubstring should not be invoked";}
void PureHuffmanCompressionModel::endEncoding() { throw "PureHuffmanModel::endEncoding should not be invoked";}

void PureHuffmanCompressionModel::compress(const char *buf, int length, ostream& out) {
    HuffmanEncoder<FrequencyHuffmanModel> encoder(out, *codeModel);
    for (int i = 0; i < length; i++) {
        encoder.encodeSymbol(((int)buf[i]) & 0xff);
    }
    encoder.finish();
}


void PureHuffmanCompressionModel::decompress(const char *buf, int length, ostream& out){
    NoCopyReadOnlyStreamBuf readBuf(const_cast<char *>(buf), length);
    istream in(&readBuf);

    HuffmanDecoder<FrequencyHuffmanModel> decoder(in, *codeModel);

    int nextSymbol;
    while ((nextSymbol = decoder.decodeSymbol()) != -1) {
        out.put((char)nextSymbol);
    }
}

}

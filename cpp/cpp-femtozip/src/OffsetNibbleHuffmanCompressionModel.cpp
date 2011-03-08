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
 * OffsetNibbleHuffmanCompressionModel.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include <vector>
#include "OffsetNibbleHuffmanCompressionModel.h"
#include "HuffmanDecoder.h"
#include "NoCopyReadOnlyStreamBuf.h"
#include "SubstringUnpacker.h"

using namespace std;

namespace femtozip {

struct OffsetNibbleHuffmanModelBuilder : public SubstringPacker::Consumer {

    vector<int> literalLengthHistogram;
    vector<int> offsetHistogramNibble0;
    vector<int> offsetHistogramNibble1;
    vector<int> offsetHistogramNibble2;
    vector<int> offsetHistogramNibble3;

    OffsetNibbleHuffmanModelBuilder() :
            literalLengthHistogram(256 + 256 + 1, 0), // 256 for each unique literal byte, 256 for all possible length, plus 1 for EOF
            offsetHistogramNibble0(16),
            offsetHistogramNibble1(16),
            offsetHistogramNibble2(16),
            offsetHistogramNibble3(16) {}


    void encodeLiteral(int aByte) {
        literalLengthHistogram[aByte]++;
    }

    void endEncoding() {
        literalLengthHistogram[literalLengthHistogram.size() - 1]++;
    }

    void encodeSubstring(int offset, int length) {

        if (length < 1 || length > 255) {
            throw "OffsetNibbleHuffmanModelBuilder::encodeSubstring: Illegal argument length out of range";
        }
        literalLengthHistogram[256 + length]++;

        offset = -offset;
        if (length < 1 || offset > (2<<15)-1) {
            throw "OffsetNibbleHuffmanModelBuilder::encodeSubstring: Illegal argument offset out of range";
        }
        offsetHistogramNibble0[offset & 0xf]++;
        offsetHistogramNibble1[(offset >> 4) & 0xf]++;
        offsetHistogramNibble2[(offset >> 8) & 0xf]++;
        offsetHistogramNibble3[(offset >> 12) & 0xf]++;
    }

    OffsetNibbleHuffmanModel *createModel() {
        return new OffsetNibbleHuffmanModel(
                new FrequencyHuffmanModel(literalLengthHistogram, false),
                new FrequencyHuffmanModel(offsetHistogramNibble0, false),
                new FrequencyHuffmanModel(offsetHistogramNibble1, false),
                new FrequencyHuffmanModel(offsetHistogramNibble2, false),
                new FrequencyHuffmanModel(offsetHistogramNibble3, false));
    }
};


OffsetNibbleHuffmanCompressionModel::OffsetNibbleHuffmanCompressionModel() {
}

OffsetNibbleHuffmanCompressionModel::~OffsetNibbleHuffmanCompressionModel() {
    if (encoder) {
        delete encoder;
    }
    if (codeModel) {
        delete codeModel;
    }
}


void OffsetNibbleHuffmanCompressionModel::build(DocumentList& documents) {
    buildDictionaryIfUnspecified(documents);
    OffsetNibbleHuffmanModelBuilder *modelBuilder = static_cast<OffsetNibbleHuffmanModelBuilder *>(buildEncodingModel(documents));
    codeModel = modelBuilder->createModel();
    delete modelBuilder;
}

SubstringPacker::Consumer *OffsetNibbleHuffmanCompressionModel::createModelBuilder() {
    return new OffsetNibbleHuffmanModelBuilder();
}

void OffsetNibbleHuffmanCompressionModel::compress(const char *buf, int length, ostream& out) {
    codeModel->reset();
    encoder = new HuffmanEncoder(out, *codeModel);
    CompressionModel::compress(buf, length, out);
    encoder->finish();
    delete encoder;
    encoder = 0;
}

void OffsetNibbleHuffmanCompressionModel::encodeLiteral(int aByte) {
    encoder->encodeSymbol(aByte);
}

void OffsetNibbleHuffmanCompressionModel::encodeSubstring(int offset, int length) {
    if (length < 1 || length > 255) {
        throw "OffsetNibbleHuffmanCompressionModel::encodeSubstring: Illegal argument length out of range";
    }
    encoder->encodeSymbol(256 + length);

    offset = -offset;
    if (offset < 1 || offset > (2<<15)-1) {
        throw "OffsetNibbleHuffmanCompressionModel::encodeSubstring: Illegal argument offset out of range";
    }
    encoder->encodeSymbol(offset & 0xf);
    encoder->encodeSymbol((offset >> 4) & 0xf);
    encoder->encodeSymbol((offset >> 8) & 0xf);
    encoder->encodeSymbol((offset >> 12) & 0xf);
}

void OffsetNibbleHuffmanCompressionModel::endEncoding() {
}

void OffsetNibbleHuffmanCompressionModel::decompress(const char *buf, int length, ostream& out) {
    NoCopyReadOnlyStreamBuf readBuf(const_cast<char *>(buf), length);
    istream in(&readBuf);

    codeModel->reset();

    HuffmanDecoder decoder(in, *codeModel);
    vector<char> outVector; //XXX performance.  Unpacker should pump right into out
    SubstringUnpacker unpacker(dict, dictLen, outVector);

    int nextSymbol;
    while ((nextSymbol = decoder.decodeSymbol()) != -1) {
        if (nextSymbol > 255) {
            int length = nextSymbol - 256;
            int offset = decoder.decodeSymbol() | (decoder.decodeSymbol() << 4) | (decoder.decodeSymbol() << 8) | (decoder.decodeSymbol() << 12);
            offset = -offset;
            unpacker.encodeSubstring(offset, length);
        }
        else {
            unpacker.encodeLiteral(nextSymbol);
        }
    }
    unpacker.endEncoding();
    out.write(&outVector[0], outVector.size()); // XXX performance - lame.
}


}

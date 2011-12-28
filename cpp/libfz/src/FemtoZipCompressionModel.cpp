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

#include <stdexcept>
#include <vector>
#include "FemtoZipCompressionModel.h"
#include "HuffmanDecoder.h"
#include "NoCopyReadOnlyStreamBuf.h"
#include "SubstringUnpacker.h"
#include "FemtoZipHuffmanModel.h"

using namespace std;

namespace femtozip {

struct FemtoZipHuffmanModelBuilder : public SubstringPacker::Consumer {

    vector<int> literalLengthHistogram;
    vector<int> offsetHistogramNibble0;
    vector<int> offsetHistogramNibble1;
    vector<int> offsetHistogramNibble2;
    vector<int> offsetHistogramNibble3;

    FemtoZipHuffmanModelBuilder() :
            literalLengthHistogram(256 + 256 + 1, 0), // 256 for each unique literal byte, 256 for all possible length, plus 1 for EOF
            offsetHistogramNibble0(16),
            offsetHistogramNibble1(16),
            offsetHistogramNibble2(16),
            offsetHistogramNibble3(16) {}


    void encodeLiteral(int aByte, void *context) {
        literalLengthHistogram[aByte]++;
    }

    void endEncoding(void *context) {
        literalLengthHistogram[literalLengthHistogram.size() - 1]++;
    }

    void encodeSubstring(int offset, int length, void *context) {

        if (length < 1 || length > 255) {
            throw runtime_error("FemtoZipHuffmanModelBuilder::encodeSubstring: Illegal argument length out of range");
        }
        literalLengthHistogram[256 + length]++;

        offset = -offset;
        if (length < 1 || offset > (2<<15)-1) {
            throw runtime_error("FemtoZipHuffmanModelBuilder::encodeSubstring: Illegal argument offset out of range");
        }
        offsetHistogramNibble0[offset & 0xf]++;
        offsetHistogramNibble1[(offset >> 4) & 0xf]++;
        offsetHistogramNibble2[(offset >> 8) & 0xf]++;
        offsetHistogramNibble3[(offset >> 12) & 0xf]++;
    }

    void createModel(FrequencyHuffmanModel *&literalLengthModel, FrequencyHuffmanModel *&offsetNibble0Model, FrequencyHuffmanModel *&offsetNibble1Model, FrequencyHuffmanModel *&offsetNibble2Model, FrequencyHuffmanModel *&offsetNibble3Model) {
        literalLengthModel = new FrequencyHuffmanModel(literalLengthHistogram, false);
        offsetNibble0Model = new FrequencyHuffmanModel(offsetHistogramNibble0, false);
        offsetNibble1Model = new FrequencyHuffmanModel(offsetHistogramNibble1, false);
        offsetNibble2Model = new FrequencyHuffmanModel(offsetHistogramNibble2, false);
        offsetNibble3Model = new FrequencyHuffmanModel(offsetHistogramNibble3, false);
    }
};


FemtoZipCompressionModel::FemtoZipCompressionModel() : literalLengthModel(0), offsetNibble0Model(0), offsetNibble1Model(0), offsetNibble2Model(0), offsetNibble3Model(0) {
}

FemtoZipCompressionModel::~FemtoZipCompressionModel() {
    if (literalLengthModel) {
        delete literalLengthModel;
        delete offsetNibble0Model;
        delete offsetNibble1Model;
        delete offsetNibble2Model;
        delete offsetNibble3Model;
    }
}

void FemtoZipCompressionModel::load(DataInput& in) {
    CompressionModel::load(in);
    bool hasModel;
    in >> hasModel;
    if (hasModel) {
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
}

void FemtoZipCompressionModel::save(DataOutput& out) {
    CompressionModel::save(out);
    out << (literalLengthModel ? true : false);
    if (literalLengthModel) {
        literalLengthModel->save(out);
        offsetNibble0Model->save(out);
        offsetNibble1Model->save(out);
        offsetNibble2Model->save(out);
        offsetNibble3Model->save(out);
    }
}

void FemtoZipCompressionModel::build(DocumentList& documents) {
    buildDictionaryIfUnspecified(documents);
    FemtoZipHuffmanModelBuilder *modelBuilder = static_cast<FemtoZipHuffmanModelBuilder *>(buildEncodingModel(documents));
    modelBuilder->createModel(literalLengthModel, offsetNibble0Model, offsetNibble1Model, offsetNibble2Model, offsetNibble3Model);
    delete modelBuilder;
}

SubstringPacker::Consumer *FemtoZipCompressionModel::createModelBuilder() {
    return new FemtoZipHuffmanModelBuilder();
}

void FemtoZipCompressionModel::compress(const char *buf, int length, ostream& out) {
    vector<char> outbuf; // Should we propagate this up to the outer levels?
    outbuf.reserve(length);
    FemtoZipHuffmanModel model(this);
    HuffmanEncoder<FemtoZipHuffmanModel> *encoder = new HuffmanEncoder<FemtoZipHuffmanModel>(outbuf, model);
    getSubstringPacker()->pack(buf, length, *this, encoder);
    encoder->finish();
    delete encoder;
    if (outbuf.size() > 0) {
        out.write(&outbuf[0], outbuf.size());
    }
}

void FemtoZipCompressionModel::encodeLiteral(int aByte, void *context) {
    HuffmanEncoder<FemtoZipHuffmanModel> *encoder = reinterpret_cast<HuffmanEncoder<FemtoZipHuffmanModel> *>(context);
    encoder->encodeSymbol(aByte);
}

void FemtoZipCompressionModel::encodeSubstring(int offset, int length, void *context) {
    HuffmanEncoder<FemtoZipHuffmanModel> *encoder = reinterpret_cast<HuffmanEncoder<FemtoZipHuffmanModel> *>(context);
    if (length < 1 || length > 255) {
        throw runtime_error("FemtoZipCompressionModel::encodeSubstring: Illegal argument length out of range");
    }
    encoder->encodeSymbol(256 + length);

    offset = -offset;
    if (offset < 1 || offset > (2<<15)-1) {
        throw runtime_error("FemtoZipCompressionModel::encodeSubstring: Illegal argument offset out of range");
    }
    encoder->encodeSymbol(offset & 0xf);
    encoder->encodeSymbol((offset >> 4) & 0xf);
    encoder->encodeSymbol((offset >> 8) & 0xf);
    encoder->encodeSymbol((offset >> 12) & 0xf);
}

void FemtoZipCompressionModel::endEncoding(void *context) {
}

void FemtoZipCompressionModel::decompress(const char *buf, int length, ostream& out) {
    FemtoZipHuffmanModel model(this);
    HuffmanDecoder<FemtoZipHuffmanModel> decoder(buf, length, model);
    vector<char> outVector; //XXX performance.  Unpacker should pump right into out
    outVector.reserve(4 * length);
    SubstringUnpacker unpacker(dict, dictLen, outVector);
    unpacker.reserve(5*length);

    int nextSymbol;
    while ((nextSymbol = decoder.decodeSymbol()) != -1) {
        if (nextSymbol > 255) {
            int length = nextSymbol - 256;
            int offset = decoder.decodeSymbol() | (decoder.decodeSymbol() << 4) | (decoder.decodeSymbol() << 8) | (decoder.decodeSymbol() << 12);
            offset = -offset;
            unpacker.encodeSubstring(offset, length, 0);
        }
        else {
            unpacker.encodeLiteral(nextSymbol, 0);
        }
    }
    unpacker.endEncoding(0);
    out.write(&outVector[0], outVector.size()); // XXX performance - lame.
}


}

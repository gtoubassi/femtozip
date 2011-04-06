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
 * CompressionModel.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include <iostream>
#include <sstream>
#include <string.h>
#include "CompressionModel.h"
#include "DictionaryOptimizer.h"
#include "PureHuffmanCompressionModel.h"
#include "FemtoZipCompressionModel.h"
#include "GZipCompressionModel.h"
#include "GZipDictionaryCompressionModel.h"
#include "SamplingDocumentList.h"

using namespace std;

namespace femtozip {

CompressionModel *CompressionModel::createModel(const string& type) {
    if (type == "PureHuffman") {
        return new PureHuffmanCompressionModel();
    }
    else if (type == "FemtoZip") {
        return new FemtoZipCompressionModel();
    }
    else if (type == "GZip") {
        return new GZipCompressionModel();
    }
    else if (type == "GZipDictionary") {
        return new GZipDictionaryCompressionModel();
    }
    else {
        throw "Unknown model";
    }
}

void CompressionModel::saveModel(CompressionModel& model, DataOutput& out) {
    out << string(model.typeName());
    model.save(out);
}

CompressionModel *CompressionModel::buildOptimalModel(DocumentList& documents, bool verify, vector<string> *modelTypes) {
    vector<CompressionModel *> models;
    if (modelTypes) {
        for (vector<string>::iterator i = modelTypes->begin(); i != modelTypes->end(); i++) {
            models.push_back(CompressionModel::createModel(*i));
        }
    }
    else {
        models.push_back(new FemtoZipCompressionModel());
        models.push_back(new PureHuffmanCompressionModel());
    }

    // Split the documents into two groups.  One for building each model out
    // and one for testing which model is best.  Shouldn't build and test
    // with the same set as a model may over optimize for the training set.
    SamplingDocumentList trainingDocuments(documents, 2, 0);
    SamplingDocumentList testingDocuments(documents, 2, 1);

    // Build each model

    const char *builtDict = 0;
    int builtDictLength = 0;
    for (vector<CompressionModel *>::iterator i = models.begin(); i != models.end(); i++) {
        if (builtDict) {
            // This is an optimization to avoid building multiple times
            // though technically if different models built the dict differently we'd be hosed.
            (*i)->setDictionary(builtDict, builtDictLength);
        }
        (*i)->build(trainingDocuments);
        if (!builtDict) {
            builtDict = (*i)->getDictionary(builtDictLength);
        }
    }

    // Pick the best one
    vector<size_t> compressedSizes(models.size());
    for (int i = 0, count = testingDocuments.size(); i < count; i++) {
        int length;
        const char *data = testingDocuments.get(i, length);

        for (vector<CompressionModel *>::iterator i = models.begin(); i != models.end(); i++) {
            ostringstream out;
            (*i)->compress(data, length, out);
            string outstr = out.str();

            if (verify) {
                ostringstream decompressedOut;
                (*i)->decompress(outstr.c_str(), outstr.length(), decompressedOut);
                string decompressed = decompressedOut.str();
                if (decompressed.length() != static_cast<unsigned int>(length) || memcmp(decompressed.c_str(), data, length) != 0) {
                    throw "Compress/Decompress roundtrip failed";
                }
            }

            compressedSizes[i - models.begin()] += outstr.length();
        }
    }

    size_t i;
    size_t bestIndex = 0;
    for (i = 1; i < compressedSizes.size(); i++) {
        if (compressedSizes[bestIndex] > compressedSizes[i]) {
            bestIndex = i;
        }
    }

    for (i = 0; i < models.size(); i++) {
        if (i != bestIndex) {
            delete models[i];
        }
    }

    return models[bestIndex];
}

CompressionModel *CompressionModel::loadModel(DataInput& in) {
    string type;
    in >> type;
    CompressionModel *model = createModel(type);
    model->load(in);
    return model;
}

CompressionModel::CompressionModel(int level) : dict(0), dictLen(0), packer(0) {
    setCompressionLevel(level);
}

CompressionModel::~CompressionModel() {
    if (packer) {
        delete packer;
        packer = 0;
    }
    if (dict) {
        delete[] dict;
    }
}

void CompressionModel::setCompressionLevel(int level) {
    compressionLevel = min(9, max(0, level));
}

int CompressionModel::getCompressionLevel() {
    return compressionLevel;
}

void CompressionModel::load(DataInput& in) {
    int version;
    in >> version;

    int length;
    in >> length;
    char *d = 0;
    if (length > 0) {
        d = new char[length];
        in.read(d, length);
    }
    setDictionary(d, length);
    if (d) {
        delete[] d;
    }
}

void CompressionModel::save(DataOutput& out) {
    out << 0; // poor mans file format version

    out << dictLen;
    if (dictLen > 0) {
        out.write(dict, dictLen);
    }
}

void CompressionModel::setDictionary(const char *dictionary, int length) {
    if (dict) {
        delete dict;
        dict = 0;
        dictLen = 0;
    }
    if (length) {
        char *d = new char[length];
        memcpy(d, dictionary, length);
        dict = d;
        dictLen = length;
    }
    else {
        dict = 0;
        dictLen = 0;
    }
    if (packer) {
        delete packer;
        packer = 0;
    }
}

SubstringPacker *CompressionModel::getSubstringPacker() {
    if (!packer) {
        packer = new SubstringPacker(dict, dictLen, compressionLevel);
    }
    return packer;
}


const char *CompressionModel::getDictionary(int& length) {
    length = dictLen;
    return dict;
}

void CompressionModel::setMaxDictionary(int maxDictionary) {
    if (dictLen > 0 && dictLen > maxDictionary) {
        setDictionary(dict + dictLen - maxDictionary, maxDictionary);
    }
}

void CompressionModel::compress(const char *buf, int length, ostream& out) {
    getSubstringPacker()->pack(buf, length, *this, 0);
}

SubstringPacker::Consumer *CompressionModel::createModelBuilder() {
    return 0;
}


SubstringPacker::Consumer *CompressionModel::buildEncodingModel(DocumentList& documents) {
    SubstringPacker modelBuildingPacker(dict, dictLen);
    SubstringPacker::Consumer *modelBuilder = createModelBuilder();

    for (int i = 0, count = documents.size(); i < count; i++) {
        int length;
        const char * docBytes = documents.get(i, length);
        modelBuildingPacker.pack(docBytes, length, *modelBuilder, 0);
        documents.release(docBytes);
    }

    return modelBuilder;
}

void CompressionModel::buildDictionaryIfUnspecified(DocumentList& documents) {
    if (!dict) {
        DictionaryOptimizer optimizer(documents);
        string dictionary = optimizer.optimize(64*1024);
        setDictionary(dictionary.c_str(), dictionary.length());
    }
}

}

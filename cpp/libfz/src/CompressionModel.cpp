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
#include <string.h>
#include "CompressionModel.h"
#include "DictionaryOptimizer.h"
#include "PureHuffmanCompressionModel.h"
#include "OffsetNibbleHuffmanCompressionModel.h"
#include "GZipCompressionModel.h"
#include "GZipDictionaryCompressionModel.h"

using namespace std;

namespace femtozip {

CompressionModel *CompressionModel::createModel(const string& type) {
    if (type == "PureHuffman") {
        return new PureHuffmanCompressionModel();
    }
    else if (type == "OffsetNibbleHuffman") {
        return new OffsetNibbleHuffmanCompressionModel();
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

CompressionModel *CompressionModel::loadModel(DataInput& in) {
    string type;
    in >> type;
    CompressionModel *model = createModel(type);
    model->load(in);
    return model;
}

CompressionModel::CompressionModel() : dict(0), dictLen(0), packer(0) {
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
        packer = new SubstringPacker(dict, dictLen);
    }
    return packer;
}


const char *CompressionModel::getDictionary(int& length) {
    length = dictLen;
    return dict;
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

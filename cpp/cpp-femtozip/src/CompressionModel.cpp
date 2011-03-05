/*
 * CompressionModel.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include "CompressionModel.h"

namespace femtozip {

CompressionModel::CompressionModel() : dict(0), dictLen(0) {

}

CompressionModel::~CompressionModel() {
}

void CompressionModel::setDictionary(const char *dictionary, int length) {
    dict = dictionary;
    dictLen = length;
}

const char *CompressionModel::getDictionary(int& length) {
    length = dictLen;
    return dict;
}

void CompressionModel::compress(const char *buf, int length, ostream& out) {
    SubstringPacker packer(dict, dictLen);
    packer.pack(buf, length, *this);

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
        modelBuildingPacker.pack(docBytes, length, *modelBuilder);
    }

    return modelBuilder;
}

void CompressionModel::buildDictionaryIfUnspecified(DocumentList& documents) {
}


}

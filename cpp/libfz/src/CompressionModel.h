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
 * CompressionModel.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef COMPRESSIONMODEL_H_
#define COMPRESSIONMODEL_H_

#include <iostream>
#include "SubstringPacker.h"
#include "DocumentList.h"
#include "DataIO.h"

namespace femtozip {

class CompressionModel : public SubstringPacker::Consumer {
protected:
    const char *dict;
    int dictLen;
    SubstringPacker *packer;

    virtual void buildDictionaryIfUnspecified(DocumentList& documents);
    SubstringPacker *getSubstringPacker();

public:
    static CompressionModel *createModel(const string& type);
    static void saveModel(CompressionModel& model, DataOutput& out);
    static CompressionModel *loadModel(DataInput& in);

    CompressionModel();
    virtual ~CompressionModel();

    virtual void load(DataInput& in);
    virtual void save(DataOutput& out);

    virtual const char *typeName() = 0;

    virtual void setDictionary(const char *dictionary, int length);
    virtual const char *getDictionary(int& length);

    virtual void build(DocumentList& documents) = 0;

    virtual void compress(const char *buf, int length, ostream& out);
    virtual void decompress(const char *buf, int length, ostream& out) = 0;

    virtual void encodeLiteral(int aByte, void *context) = 0;
    virtual void encodeSubstring(int offset, int length, void *context) = 0;
    virtual void endEncoding(void *context) = 0;

    virtual SubstringPacker::Consumer *createModelBuilder();

    virtual SubstringPacker::Consumer *buildEncodingModel(DocumentList& documents);
};

}

#endif /* COMPRESSIONMODEL_H_ */

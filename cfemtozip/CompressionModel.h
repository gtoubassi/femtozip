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

namespace femtozip {

class CompressionModel : public SubstringPacker::Consumer {
protected:
    const char *dict;
    int dictLen;

    virtual void buildDictionaryIfUnspecified(DocumentList& documents);

public:
    CompressionModel();
    virtual ~CompressionModel();

    virtual void setDictionary(const char *dictionary, int length);
    virtual const char *getDictionary(int& length);

    virtual void build(DocumentList& documents) = 0;

    virtual void compress(const char *buf, int length, ostream& out);
    virtual void decompress(const char *buf, int length, ostream& out) = 0;

    virtual void encodeLiteral(int aByte) = 0;
    virtual void encodeSubstring(int offset, int length) = 0;
    virtual void endEncoding() = 0;

    virtual SubstringPacker::Consumer *createModelBuilder();

    virtual SubstringPacker::Consumer *buildEncodingModel(DocumentList& documents);
};

}

#endif /* COMPRESSIONMODEL_H_ */

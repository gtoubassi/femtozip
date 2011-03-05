/*
 * PureHuffmanModel.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef PUREHUFFMANMODEL_H_
#define PUREHUFFMANMODEL_H_

#include <iostream>
#include "CompressionModel.h"
#include "DocumentList.h"
#include "FrequencyHuffmanModel.h"

using namespace std;

namespace femtozip {

class PureHuffmanCompressionModel : public CompressionModel {

protected:
    FrequencyHuffmanModel *codeModel;

public:
    PureHuffmanCompressionModel();
    virtual ~PureHuffmanCompressionModel();

    virtual void build(DocumentList& documents);
    virtual void encodeLiteral(int aByte);
    virtual void encodeSubstring(int offset, int length);
    virtual void endEncoding();

    virtual void compress(const char *buf, int length, ostream& out);
    virtual void decompress(const char *buf, int length, ostream& out);
};

}

#endif /* PUREHUFFMANMODEL_H_ */

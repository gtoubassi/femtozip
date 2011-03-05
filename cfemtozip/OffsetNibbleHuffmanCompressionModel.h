/*
 * OffsetNibbleHuffmanCompressionModel.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef OFFSETNIBBLEHUFFMANCOMPRESSIONMODEL_H_
#define OFFSETNIBBLEHUFFMANCOMPRESSIONMODEL_H_

#include <iostream>

#include "CompressionModel.h"
#include "DocumentList.h"
#include "HuffmanEncoder.h"
#include "OffsetNibbleHuffmanModel.h"
#include "SubstringPacker.h"

namespace femtozip {

class OffsetNibbleHuffmanCompressionModel : public CompressionModel {
private:
    OffsetNibbleHuffmanModel *codeModel;
    HuffmanEncoder *encoder; // XXX threading

public:
    OffsetNibbleHuffmanCompressionModel();
    virtual ~OffsetNibbleHuffmanCompressionModel();

    virtual void build(DocumentList& documents);
    virtual SubstringPacker::Consumer *createModelBuilder();

    virtual void compress(const char *buf, int length, ostream& out);
    virtual void decompress(const char *buf, int length, ostream& out);

    virtual void encodeLiteral(int aByte);
    virtual void encodeSubstring(int offset, int length);
    virtual void endEncoding();







};

}

#endif /* OFFSETNIBBLEHUFFMANCOMPRESSIONMODEL_H_ */

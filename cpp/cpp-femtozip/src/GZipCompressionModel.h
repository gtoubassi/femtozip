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

#ifndef GZIPCOMPRESSIONMODEL_H_
#define GZIPCOMPRESSIONMODEL_H_

#include <iostream>
#include "CompressionModel.h"
#include "DocumentList.h"

using namespace std;

namespace femtozip {

class GZipCompressionModel : public CompressionModel {
public:
    GZipCompressionModel();
    virtual ~GZipCompressionModel();

    virtual const char *typeName() { return "GZip"; };

    virtual void build(DocumentList& documents);
    virtual void encodeLiteral(int aByte);
    virtual void encodeSubstring(int offset, int length);
    virtual void endEncoding();

    virtual void compress(const char *buf, int length, ostream& out);
    virtual void decompress(const char *buf, int length, ostream& out);

protected:
    virtual bool useDict();
};

}

#endif /* GZIPCOMPRESSIONMODEL_H_ */

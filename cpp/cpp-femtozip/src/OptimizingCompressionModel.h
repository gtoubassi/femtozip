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
 * OptimizingCompressionModel.h
 *
 *  Created on: Mar 8, 2011
 *      Author: gtoubassi
 */

#ifndef OPTIMIZINGCOMPRESSIONMODEL_H_
#define OPTIMIZINGCOMPRESSIONMODEL_H_

#include <string>
#include <vector>
#include "CompressionModel.h"
#include "DataIO.h"

using namespace std;

namespace femtozip {

class OptimizingCompressionModel : public CompressionModel {
public:
    class CompressionResult {
    public:
        CompressionModel *model;
        int totalCompressedSize;
        int totalDataSize;

        CompressionResult(CompressionModel *model) : model(model), totalCompressedSize(0), totalDataSize(0) {}

        void accumulate(CompressionResult& result) {
            totalCompressedSize += result.totalCompressedSize < result.totalDataSize ? result.totalCompressedSize : result.totalDataSize;
            totalDataSize += result.totalDataSize;
        }

        bool operator<(const CompressionResult& other) const {
            return totalCompressedSize < other.totalCompressedSize;
        }
    };

    OptimizingCompressionModel();
    OptimizingCompressionModel(vector<string>& models);

    virtual ~OptimizingCompressionModel();

    virtual void load(DataInput& in);
    virtual void save(DataOutput& out);

    virtual const char *typeName() { return "Optimizing"; };

    virtual void build(DocumentList& documents);
    virtual void optimize(DocumentList& documents);

    virtual void compress(const char *buf, int length, ostream& out);
    virtual void decompress(const char *buf, int length, ostream& out);

    virtual void encodeLiteral(int aByte);
    virtual void encodeSubstring(int offset, int length);
    virtual void endEncoding();

    virtual void aggregateResults(vector<CompressionResult> &aggregateResults);

    CompressionModel *getBestPerformingModel();
    OptimizingCompressionModel::CompressionResult &getBestPerformingResult();

private:
    vector<CompressionResult> results;
    vector<CompressionResult> sortedResults;
    int totalDataSize;
};

}

#endif /* OPTIMIZINGCOMPRESSIONMODEL_H_ */

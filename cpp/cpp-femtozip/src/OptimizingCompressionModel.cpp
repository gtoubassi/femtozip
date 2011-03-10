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
 * OptimizingCompressionModel.cpp
 *
 *  Created on: Mar 8, 2011
 *      Author: gtoubassi
 */

#include <algorithm>
#include <iostream>
#include <sstream>
#include <string>
#include "OptimizingCompressionModel.h"
#include "OffsetNibbleHuffmanCompressionModel.h"
#include "PureHuffmanCompressionModel.h"

using namespace std;

namespace femtozip {

OptimizingCompressionModel::OptimizingCompressionModel() {
    results.push_back(CompressionResult(new OffsetNibbleHuffmanCompressionModel()));
    results.push_back(CompressionResult(new PureHuffmanCompressionModel()));
}

OptimizingCompressionModel::OptimizingCompressionModel(vector<string>& models) {
    for (vector<string>::iterator i = models.begin(); i != models.end(); i++) {
        CompressionModel *model;

        if (*i == "PureHuffman") {
            model = new PureHuffmanCompressionModel();
        }
        else if (*i == "OFfsetNibbleHuffman") {
            model = new OffsetNibbleHuffmanCompressionModel();
        }
        else {
            throw "Unknown model";
        }
        CompressionResult result(model);
        results.push_back(result);
    }
}

OptimizingCompressionModel::~OptimizingCompressionModel() {
    for (vector<CompressionResult>::iterator i = results.begin(); i != results.end(); i++) {
        delete i->model;
    }
}

void OptimizingCompressionModel::build(DocumentList& documents) {
    buildDictionaryIfUnspecified(documents);

    for (vector<CompressionResult>::iterator result = results.begin(); result != results.end(); result++) {

        // No need to recompute this over and over.  This assumes all types of compression model
        // compute the dictionary the same way.
        int dictLength;
        const char *dict = getDictionary(dictLength);
        result->model->setDictionary(dict, dictLength);
        result->model->build(documents);
    }
}

void OptimizingCompressionModel::optimize(DocumentList& documents) {
    for (int i = 0, count = documents.size(); i < count; i++) {
        int length;
        const char *buf = documents.get(i, length);

        totalDataSize += length;

        for (vector<CompressionResult>::iterator result = results.begin(); result != results.end(); result++) {
            ostringstream compressOut;
            result->model->compress(buf, length, compressOut);
            string compressed = compressOut.str();

            if (true) {
                ostringstream decompressOut;
                result->model->decompress(compressed.c_str(), compressed.length(), decompressOut);
                string decompressed = decompressOut.str();
                if (decompressed.length() != static_cast<unsigned int>(length) || memcmp(decompressed.c_str(), buf, length) != 0) {
                    throw "Compress/Decompress roundtrip failed";
                }
            }

            result->totalCompressedSize += compressed.size();
            result->totalDataSize += length;
        }
    }

    sortedResults = results;
    sort(sortedResults.begin(), sortedResults.end());
}


void OptimizingCompressionModel::compress(const char *buf, int length, ostream& out) {
    getBestPerformingModel()->compress(buf, length, out);
}

void OptimizingCompressionModel::decompress(const char *buf, int length, ostream& out) {
    getBestPerformingModel()->decompress(buf, length, out);
}

CompressionModel *OptimizingCompressionModel::getBestPerformingModel() {
    int best = sortedResults[0].totalCompressedSize;
    return best > totalDataSize ? 0 : sortedResults[0].model;
}

OptimizingCompressionModel::CompressionResult &OptimizingCompressionModel::getBestPerformingResult() {
    return sortedResults[0];
}

void OptimizingCompressionModel::encodeLiteral(int aByte) {
    throw "OptimizingCompressionModel::encodeLiteral unsupported";
}

void OptimizingCompressionModel::encodeSubstring(int offset, int length) {
    throw "OptimizingCompressionModel::encodeSubstring unsupported";
}

void OptimizingCompressionModel::endEncoding() {
    throw "OptimizingCompressionModel::endEncoding unsupported";
}

void OptimizingCompressionModel::aggregateResults(vector<CompressionResult> &aggregateResults) {
    if (aggregateResults.size() == 0) {
        for (vector<CompressionResult>::iterator result = results.begin(); result != results.end(); result++) {
            CompressionResult r(result->model);
            aggregateResults.push_back(r);
        }
    }
    for (int i = 0, count = results.size(); i < count; i++) {
        CompressionResult& result = results[i];
        CompressionResult& aggregate = aggregateResults[i];

        aggregate.accumulate(result);
    }
}


}

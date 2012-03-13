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
 * FrequencyHuffmanModel.cpp
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#include <algorithm>
#include <limits>
#include "FrequencyHuffmanModel.h"

using namespace std;

namespace femtozip {

class HuffmanNode {
public:
    HuffmanNode *left;
    HuffmanNode *right;
    int weight;
    int symbol;

    HuffmanNode(HuffmanNode *left, HuffmanNode *right, int weight, int symbol = -1) : left(left), right(right), weight(weight), symbol(symbol) {};

    ~HuffmanNode() {
        if (left) {
            delete left;
        }
        if (right) {
            delete right;
        }
    };

    void collectPrefixes(vector<Codeword>& prefixes, Codeword& current) {
        if (symbol == -1) {
            Codeword c = current;
            c.appendBit(0);
            left->collectPrefixes(prefixes, c);
            c = current;
            c.appendBit(1);
            right->collectPrefixes(prefixes, c);
        }
        else {
            prefixes[symbol] = current;
            prefixes[symbol].symbol = symbol;
        }
    }
};

bool huffmanNodeCompare(const HuffmanNode *n1, const HuffmanNode *n2) { return n1->weight < n2->weight; }

void FrequencyHuffmanModel::computeHistogramWithEOFSymbol(vector<int>& histogram, const char *data, int len) {
    histogram.resize(256 + 1); // +1 for EOF
    for (int i = 0; i < len; i++) {
        histogram[((int)data[i]) & 0xff]++;
    }
    histogram[histogram.size() - 1] = 1; // EOF
}

FrequencyHuffmanModel::FrequencyHuffmanModel() {
}

FrequencyHuffmanModel::FrequencyHuffmanModel(vector<int>& histogram, bool allSymbolsSampled) {
    if (!allSymbolsSampled) {
        for (vector<int>::iterator i = histogram.begin(); i != histogram.end(); i++) {
            if (*i == 0) {
                *i = 1;
            }
        }
    }

    computeHuffmanCoding(histogram);
}

FrequencyHuffmanModel::~FrequencyHuffmanModel() {
}


void FrequencyHuffmanModel::load(DataInput& in) {
    int len;
    in >> len;
    encoding.resize(len);
    for (vector<Codeword>::iterator i = encoding.begin(); i != encoding.end(); i++) {
        i->load(in);
    }
    decoding.build(encoding);
}

void FrequencyHuffmanModel::save(DataOutput& out) {
    out << ((int)encoding.size());
    for (vector<Codeword>::iterator i = encoding.begin(); i != encoding.end(); i++) {
        i->save(out);
    }
}

void FrequencyHuffmanModel::computeHuffmanCoding(vector<int>& histogram) {
    vector<HuffmanNode *> queue1;
    vector<HuffmanNode *> queue2;

    for (int i = 0, count = histogram.size(); i < count; i++) {
        if (histogram[i] != 0) {
            queue1.push_back(new HuffmanNode(0, 0, histogram[i], i));
        }
    }

    sort(queue1.begin(), queue1.end(), huffmanNodeCompare);

    while (queue1.size() + queue2.size() > 1) {
        vector<HuffmanNode *> *candidateQueue1 = 0;
        vector<HuffmanNode *> *candidateQueue2 = 0;
        int candidateWeight = numeric_limits<int>::max();

        if (queue1.size() > 0 && queue2.size() > 0) {
            if (queue1[0]->weight + queue2[0]->weight < candidateWeight) {
                candidateWeight = queue1[0]->weight + queue2[0]->weight;
                candidateQueue1 = &queue1;
                candidateQueue2 = &queue2;
            }
        }
        if (queue1.size() > 1) {
            if (queue1[0]->weight + queue1[1]->weight < candidateWeight) {
                candidateWeight = queue1[0]->weight + queue1[1]->weight;
                candidateQueue1 = candidateQueue2 = &queue1;
            }
        }
        if (queue2.size() > 1) {
            if (queue2[0]->weight + queue2[1]->weight < candidateWeight) {
                candidateWeight = queue2[0]->weight + queue2[1]->weight;
                candidateQueue1 = candidateQueue2 = &queue2;
            }
        }

        HuffmanNode *left = (*candidateQueue1)[0];
        candidateQueue1->erase(candidateQueue1->begin(), candidateQueue1->begin() + 1);
        HuffmanNode *right = (*candidateQueue2)[0];
        candidateQueue2->erase(candidateQueue2->begin(), candidateQueue2->begin() + 1);
        HuffmanNode *newNode = new HuffmanNode(left, right, candidateWeight);
        queue2.push_back(newNode);
    }

    encoding.resize(histogram.size());
    Codeword codeword;
    queue2[0]->collectPrefixes(encoding, codeword);
    decoding.build(encoding);
    delete queue2[0];
}

}

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
 * SubstringUnpacker.cpp
 *
 *  Created on: Mar 3, 2011
 *      Author: gtoubassi
 */

#include <iostream>
#include "SubstringUnpacker.h"

using namespace std;

namespace femtozip {

SubstringUnpacker::SubstringUnpacker(const char *dictionary, int length, vector<char>& output) : out(output) {
    dict = dictionary;
    dictLen = length;
}


SubstringUnpacker::~SubstringUnpacker() {
}

void SubstringUnpacker::reserve(int size) {
    out.reserve(size);
}

void SubstringUnpacker::encodeSubstring(int offset, int length, void *context) {
    int currentIndex = static_cast<int>(out.size());
    if (currentIndex + offset < 0) {
        const char *startDict = dict + currentIndex + offset + dictLen;
        const char *endDict = startDict + length;
        int end = 0;

        if (endDict > dict + dictLen) {
            end = static_cast<int>(endDict - dict - dictLen);
            endDict = dict + dictLen;
        }
        while (startDict < endDict) {
            out.push_back(*startDict);
            startDict++;
        }

        if (end > 0) {
            out.reserve(out.size() + end); // To avoid invalidating iterators while appending (we are iterating and appending at the same time)
            for (vector<char>::iterator i = out.begin(), e = out.begin() + end; i < e; i++) {
                out.push_back(*i);
            }
        }

    }
    else {
        out.reserve(out.size() + length); // To avoid invalidating iterators while appending (we are iterating and appending at the same time)
        for (vector<char>::iterator i = out.begin() + currentIndex + offset, e = out.begin() + currentIndex + offset + length; i < e; i++) {
            out.push_back(*i);
        }
    }
}

void SubstringUnpacker::endEncoding(void *context) {
}


}

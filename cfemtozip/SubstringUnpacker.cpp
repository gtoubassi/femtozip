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

void SubstringUnpacker::encodeLiteral(int aByte) {
    out.push_back((char)aByte);
}

void SubstringUnpacker::encodeSubstring(int offset, int length) {
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

void SubstringUnpacker::endEncoding() {
}


}

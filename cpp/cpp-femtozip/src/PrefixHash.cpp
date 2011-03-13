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

#include "PrefixHash.h"
#include <iostream>

using namespace std;

namespace femtozip {

PrefixHash::PrefixHash(const char *buf, int length, bool addToHash) : buf(buf), length(length) {
    hashCapacity = 1.5 * length;
    hash = new int[hashCapacity];
    memset(hash, -1, hashCapacity * sizeof(int));
    heap = new int[length];
    memset(heap, -1, length * sizeof(int));
    if (addToHash) {
        for (const char *p = buf, *end = buf + length - PrefixLength; p < end; p++) {
            put(p);
        }
    }
}

PrefixHash::~PrefixHash() {
    if (hash) {
        delete[] hash;
    }
    if (heap) {
        delete[] heap;
    }
}

int PrefixHash::index(const char *p) {
    int index = (*reinterpret_cast<const unsigned int *>(p));
    return index % hashCapacity;
}

int PrefixHash::index(int i) {
    return index(buf + i);
}

void PrefixHash::put(const char *p) {
    int hashIndex = index(p);
    int index = (int)(p - buf);
    heap[index] = hash[hashIndex];
    hash[hashIndex] = index;
}


void PrefixHash::getBestMatch(const char *target, const char *targetBuf, int targetBufLen, const char *& bestMatch, int& bestMatchLength) {
    bestMatch = 0;
    bestMatchLength = 0;

    if (length == 0) {
        return;
    }

    int targetHashIndex = index(target);
    int candidate = hash[targetHashIndex];
    while (candidate != -1) {

        int distance;
        if (targetBuf != buf) {
            distance = length - candidate + target - targetBuf;
        }
        else {
            distance = target - targetBuf - candidate;
        }
        if (distance > (2<<15)-1) {
            // Since we are iterating over nearest offsets first, once we pass 64k
            // we know the rest are over 64k too.
            break;
        }

        const char *maxMatchJ = min(target + 255, targetBuf + targetBufLen);
        const char *maxMatchK = min(buf + candidate + 255, buf + length);
        const char *j, *k;
        for (j = target, k = buf + candidate; j < maxMatchJ && k < maxMatchK; j++, k++) {
            if (*j != *k) {
                break;
            }
        }

        int matchLength = j - target;
        if (matchLength > bestMatchLength) {
            bestMatch = buf + candidate;
            bestMatchLength = matchLength;
        }

        candidate = heap[candidate];
    }
}

}

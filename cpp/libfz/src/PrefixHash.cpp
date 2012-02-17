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

#include <iostream>
#include <string.h>
#include "PrefixHash.h"

using namespace std;

namespace femtozip {

PrefixHash::PrefixHash(const char *buf, int length, bool addToHash, int compressionLevel) : buf(buf), length(length) {
    iterationLimit = compressionLevel == 9 ? 0 : (4 << compressionLevel);
    hashCapacity = static_cast<int>(1.75 * length);
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

int PrefixHash::index(const char *p, unsigned int &asInt) {
    asInt = (*reinterpret_cast<const unsigned int *>(p));
    return asInt % hashCapacity;
}

int PrefixHash::index(int i) {
    unsigned int raw;
    return index(buf + i, raw);
}

void PrefixHash::put(const char *p) {
    unsigned int raw;
    int hashIndex = index(p, raw);
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

    int maxLimit = min(255, (int)(targetBufLen - (target - targetBuf)));

    unsigned int targetAsInt;
    int targetHashIndex = index(target, targetAsInt);
    int candidate = hash[targetHashIndex];
    int numIterations = 0;
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

        if (targetAsInt == *reinterpret_cast<const unsigned int *>(buf + candidate)) {
            const char *endJ = target + min(maxLimit, length - candidate);
            const char *j, *k;
            for (j = target + 4, k = buf + candidate + 4; j < endJ; j++, k++) {
                if (*j != *k) {
                    break;
                }
            }

            int matchLength = j - target;
            if (matchLength > bestMatchLength) {
                bestMatch = buf + candidate;
                bestMatchLength = matchLength;
            }
        }

        candidate = heap[candidate];
        if (iterationLimit && numIterations++ > iterationLimit) {
            break;
        }
    }
}

}

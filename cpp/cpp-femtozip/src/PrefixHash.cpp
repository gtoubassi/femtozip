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
    if (addToHash) {
        for (const char *p = buf, *end = buf + length - Prefix::PrefixLength; p < end; p++) {
            put(p);
        }
    }
}

PrefixHash::~PrefixHash() {
    for (hash_map<Prefix, vector<Prefix> *, HashPrefix>::iterator i = previousStrings.begin(); i != previousStrings.end(); i++) {
        delete i->second;
    }
}

inline vector<Prefix> *getPrefixLocations(hash_map<Prefix, vector<Prefix> *, HashPrefix>& table, Prefix prefix) {
    hash_map<Prefix, vector<Prefix> *, HashPrefix>::iterator i = table.find(prefix);
    if (i == table.end()) {
        vector<Prefix> *v = new vector<Prefix>;
        table[prefix] = v;
        return v;
    }
    return i->second;
}

inline vector<Prefix> *checkPrefixLocations(const hash_map<Prefix, vector<Prefix> *, HashPrefix>& table, Prefix prefix) {
    hash_map<Prefix, vector<Prefix> *, HashPrefix>::const_iterator i = table.find(prefix);
    return i == table.end() ? 0 : i->second;
}

void PrefixHash::put(const char *p) {
    Prefix prefix(p);
    vector<Prefix> *v = getPrefixLocations(previousStrings, prefix);
    v->push_back(prefix);
}


void PrefixHash::getBestMatch(const char *target, const char *targetBuf, int targetBufLen, const char *& bestMatch, int& bestMatchLength) {
    bestMatch = 0;
    bestMatchLength = 0;

    if (length == 0) {
        return;
    }

    vector<Prefix> *list = checkPrefixLocations(previousStrings, Prefix(target));
    if (!list) {
        return;
    }

    for (vector<Prefix>::reverse_iterator i = list->rbegin(); i != list->rend(); i++) {
        Prefix candidate = *i;

        int distance;
        if (targetBuf != buf) {
            distance = buf + length - candidate.prefix + target - targetBuf;
        }
        else {
            distance = target - candidate.prefix;
        }
        if (distance > (2<<15)-1) {
            // Since we are iterating over nearest offsets first, once we pass 64k
            // we know the rest are over 64k too.
            break;
        }

        const char *maxMatchJ = min(target + 255, targetBuf + targetBufLen);
        const char *maxMatchK = min(candidate.prefix + 255, buf + length);
        const char *j, *k;
        for (j = target + Prefix::PrefixLength, k = candidate.prefix + Prefix::PrefixLength; j < maxMatchJ && k < maxMatchK; j++, k++) {
            if (*j != *k) {
                break;
            }
        }

        int matchLength = j - target;
        if (matchLength > bestMatchLength) {
            bestMatch = candidate.prefix;
            bestMatchLength = matchLength;
        }
    }
}

}

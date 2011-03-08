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
 * SubstringPacker.cpp
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#include <vector>

#include "SubstringPacker.h"
#include "Prefix.h"

using namespace std;
using namespace __gnu_cxx;

/*
 * XXX Performance considerations
 *
 * 1. Don't copy the dictionary. haha
 * 2. Prehash the dictionary ala sdch
 * 2. avoid dynamic memory allocation of the prefix lists.
 * 3. avoid virtualized encoding.  use templates?
 * 4. Is the hash fast enough?  gzip uses rolling hash, we are using raw int case followed by implicit modulo?
 */
namespace femtozip {

static const int MinimumMatchLength = Prefix::PrefixLength;


SubstringPacker::SubstringPacker(const char *dictionary, int length) {
    dict = dictionary;
    dictLen = length;
}

SubstringPacker::~SubstringPacker() {
}


inline vector<Prefix> *getPrefixLocations(hash_map<Prefix, vector<Prefix> *, HashPrefix>& table, Prefix prefix) {
    vector<Prefix> *v = table[prefix];
    if (!v) {
        v = new vector<Prefix>;
        table[prefix] = v;
    }
    return v;
}

inline void addPrefixLocation(hash_map<Prefix, vector<Prefix> *, HashPrefix>& table, Prefix prefix) {
    vector<Prefix> *v = getPrefixLocations(table, prefix);
    v->push_back(prefix);
}


void SubstringPacker::pack(const char *buf, int bufLen, Consumer& consumer) {
    hash_map<Prefix, vector<Prefix> *, HashPrefix> previousStrings;

    int totalLen = dictLen + bufLen;
    char *newRawBytes = new char[totalLen];
    memcpy(newRawBytes, dict, dictLen);
    memcpy(newRawBytes + dictLen, buf, bufLen);

    buf = newRawBytes;

    for (const char *p = buf, *end = buf + dictLen - 2; p < end; p++) {
        addPrefixLocation(previousStrings, Prefix(p));
    }

    const char *previousMatch = 0;
    int previousMatchLength = 0;

    const char *curr;
    const char *end;

    for (curr = buf + dictLen, end = buf + totalLen; curr < end; curr++) {
        const char *bestMatch = 0;
        int bestMatchLength = 0;

        if (curr + Prefix::PrefixLength - 1 < end) {
            vector<Prefix> *matches = getPrefixLocations(previousStrings, Prefix(curr));

            // find the best match
            // Always check nearest indexes first.
            for (vector<Prefix>::reverse_iterator i = matches->rbegin(); i != matches->rend(); i++) {
                const char *currMatch = i->prefix;

                // Make sure we are within 64k.  This is arbitrary, but is
                // used in the symbol encoding stage (8 bits for match length, 16 bits for match offset)
                if (curr - currMatch > (2<<15)-1) {
                    // Since we are iterating over nearest offsets first, once we pass 64k
                    // we know the rest are over 64k too.
                    break;
                }

                // We know the first Prefix::PrefixLength bytes already match since they share the same prefix.
                const char *j, *k, *maxMatch;
                for (j = curr + Prefix::PrefixLength, k = currMatch + Prefix::PrefixLength, maxMatch = min(curr + 255, end); j < maxMatch; j++, k++) {
                    if (*j != *k) {
                        break;
                    }
                }

                int matchLength = k - currMatch;
                if (matchLength > bestMatchLength) {
                    bestMatch = currMatch;
                    bestMatchLength = matchLength;
                }
            }

            matches->push_back(Prefix(curr));
        }

        if (bestMatchLength < MinimumMatchLength) {
            bestMatch = 0;
            bestMatchLength = 0;
        }

        if (previousMatchLength > 0 && bestMatchLength <= previousMatchLength) {
            // We didn't get a match or we got one and the previous match is better
            consumer.encodeSubstring(-(curr - 1 - previousMatch), previousMatchLength);

            // Make sure locations are added for the match.  This allows repetitions to always
            // encode the same relative locations which is better for compressing the locations.
            //XXX
            const char *endMatch = curr - 1 + previousMatchLength;
            curr++;
            while (curr < endMatch && curr + Prefix::PrefixLength < end) {
                addPrefixLocation(previousStrings, Prefix(curr));
                curr++;
            }
            curr = endMatch - 1; // Make sure 'curr' is pointing to the last processed byte so it is at the right place in the next iteration
            previousMatch = 0;
            previousMatchLength = 0;
        }
        else if (previousMatchLength > 0 && bestMatchLength > previousMatchLength) {
            // We have a match, and we had a previous match, and this one is better.
            previousMatch = bestMatch;
            previousMatchLength = bestMatchLength;
            consumer.encodeLiteral(((int)*(curr - 1)) & 0xff);
        }
        else if (bestMatchLength > 0) {
            // We have a match, but no previous match
            previousMatch = bestMatch;
            previousMatchLength = bestMatchLength;
        }
        else if (bestMatchLength == 0 && previousMatchLength == 0) {
            // No match, and no previous match.
            consumer.encodeLiteral(((int)*curr) & 0xff);
        }
    }
    consumer.endEncoding();

    // Clean up memory

    for (hash_map<Prefix, vector<Prefix> *, HashPrefix>::iterator i = previousStrings.begin(); i != previousStrings.end(); i++) {
        delete i->second;
    }
    delete[] buf;
}

}

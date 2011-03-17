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

using namespace std;

namespace femtozip {

static const int MinimumMatchLength = PrefixHash::PrefixLength;


SubstringPacker::SubstringPacker(const char *dictionary, int length) {
    if (!dictionary) {
        dict = "";
        dictLen = 0;
    }
    else {
        dict = dictionary;
        dictLen = length;
    }
    dictHash = new PrefixHash(dict, dictLen, true);
}

SubstringPacker::~SubstringPacker() {
    if (dictHash) {
        delete dictHash;
    }
}

void SubstringPacker::pack(const char *buf, int bufLen, Consumer& consumer, void *consumerContext) {
    PrefixHash hash(buf, bufLen, false);

    const char *previousMatch = 0;
    int previousMatchLength = 0;

    const char *curr;
    const char *end;

    for (curr = buf, end = buf + bufLen; curr < end; curr++) {
        const char *bestMatch = 0;
        int bestMatchLength = 0;

        if (curr + PrefixHash::PrefixLength - 1 < end) {
            dictHash->getBestMatch(curr, buf, bufLen, bestMatch, bestMatchLength);
            const char *localMatch;
            int localMatchLength;
            hash.getBestMatch(curr, buf, bufLen, localMatch, localMatchLength);

            // Note the >= because we prefer a match that is nearer (and a match
            // in the string being compressed is always closer than one from the dict).
            if (localMatchLength >= bestMatchLength) {
                bestMatch = localMatch;
                bestMatchLength = localMatchLength;
            }

            hash.put(curr);
        }

        if (bestMatchLength < MinimumMatchLength) {
            bestMatch = 0;
            bestMatchLength = 0;
        }

        if (previousMatchLength > 0 && bestMatchLength <= previousMatchLength) {
            // We didn't get a match or we got one and the previous match is better
            if (previousMatch >= dict && previousMatch < dict + dictLen) {
                // Match is in the dictionary
                consumer.encodeSubstring(-((curr - buf) - 1 + dict + dictLen - previousMatch), previousMatchLength, consumerContext);
            }
            else {
                // Match is in the string itself (local)
                consumer.encodeSubstring(-(curr - 1 - previousMatch), previousMatchLength, consumerContext);
            }

            // Make sure locations are added for the match.  This allows repetitions to always
            // encode the same relative locations which is better for compressing the locations.
            const char *endMatch = curr - 1 + previousMatchLength;
            curr++;
            while (curr < endMatch && curr + PrefixHash::PrefixLength < end) {
                hash.put(curr);
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
            consumer.encodeLiteral(((int)*(curr - 1)) & 0xff, consumerContext);
        }
        else if (bestMatchLength > 0) {
            // We have a match, but no previous match
            previousMatch = bestMatch;
            previousMatchLength = bestMatchLength;
        }
        else if (bestMatchLength == 0 && previousMatchLength == 0) {
            // No match, and no previous match.
            consumer.encodeLiteral(((int)*curr) & 0xff, consumerContext);
        }
    }
    consumer.endEncoding(consumerContext);
}

}

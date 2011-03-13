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

#ifndef PREFIXHASH_H_
#define PREFIXHASH_H_

#include <ext/hash_map>
#include <vector>
#include "Prefix.h"

using namespace __gnu_cxx;
using namespace std;

namespace femtozip {

class PrefixHash {
protected:
    const char *buf;
    int length;
    hash_map<Prefix, vector<Prefix> *, HashPrefix> previousStrings;

public:
    PrefixHash(const char *buf, int length, bool addToHash);
    virtual ~PrefixHash();

    void getBestMatch(const char *target, const char *targetBuf, int targetBufLen, const char *& bestMatch, int& bestMatchLength);
    void put(const char *p);

    void checkit();
};

}

#endif /* PREFIXHASH_H_ */

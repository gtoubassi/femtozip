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
 * Prefix.h
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#ifndef PREFIX_H_
#define PREFIX_H_

#include <ext/hash_map>

using namespace std;

namespace femtozip {

class Prefix {

public:
    static const int PrefixLength = 4;

    const char *prefix;

    explicit inline Prefix(const char * p) { prefix = p; };

    inline bool operator==(const Prefix& p) const {
        return (*reinterpret_cast<const unsigned int *>(prefix)) == (*reinterpret_cast<const unsigned int *>(p.prefix));
    }
};

struct HashPrefix {
    size_t operator()(femtozip::Prefix p) const {
        return (size_t)(*reinterpret_cast<const unsigned int *>(p.prefix));
    }
};

inline size_t hashPrefix(const Prefix& p) {
    return (size_t)(*reinterpret_cast<const unsigned int *>(p.prefix));
}

}

/*
namespace __gnu_cxx {

template<> struct hash<femtozip::Prefix> {
    size_t operator()(femtozip::Prefix p) const {
        return (size_t)(*reinterpret_cast<const unsigned int *>(p.prefix));
    }
};

}
*/


#endif /* PREFIX_H_ */

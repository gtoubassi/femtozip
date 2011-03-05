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

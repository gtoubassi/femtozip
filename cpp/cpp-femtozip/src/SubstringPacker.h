/*
 * SubstringPacker.h
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#ifndef SUBSTRINGPACKER_H_
#define SUBSTRINGPACKER_H_

using namespace std;

#include <string>

namespace femtozip {

class SubstringPacker {

private:
    const char *dict;
    int dictLen;

public:

    class Consumer {
    public:
        virtual void encodeLiteral(int aByte) = 0;
        virtual void encodeSubstring(int offset, int length) = 0;
        virtual void endEncoding() = 0;
    };

    SubstringPacker(const char *dictionary, int length);
    ~SubstringPacker();

    void pack(const char *bytes, int length, Consumer& consumer);
};


}

#endif /* SUBSTRINGPACKER_H_ */

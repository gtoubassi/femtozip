/*
 * SubstringUnpacker.h
 *
 *  Created on: Mar 3, 2011
 *      Author: gtoubassi
 */

#ifndef SUBSTRINGUNPACKER_H_
#define SUBSTRINGUNPACKER_H_

#include <iostream>
#include <sstream>
#include <vector>
#include "SubstringPacker.h"

using namespace std;


namespace femtozip {

class SubstringUnpacker : public SubstringPacker::Consumer {

private:
    const char *dict;
    int dictLen;
    vector<char>& out;

public:
    SubstringUnpacker(const char *dictionary, int length, vector<char>& output);
    ~SubstringUnpacker();

    void encodeLiteral(int aByte);
    void encodeSubstring(int offset, int length);
    void endEncoding();
};

}

#endif /* SUBSTRINGUNPACKER_H_ */

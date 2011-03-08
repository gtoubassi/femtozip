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

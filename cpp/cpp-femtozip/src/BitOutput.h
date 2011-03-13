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
 * BitOutput.h
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#ifndef BITOUTPUT_H_
#define BITOUTPUT_H_

#include <iostream>

using namespace std;

namespace femtozip {

class BitOutput {

private:
    ostream& out;
    long buffer;
    int count;


    inline void flushBuffer() {
        while (count > 0) {
            out.put(buffer & 0xff);
            buffer >>= 8;
            count -= 8;
        }
        buffer = 0;
        count = 0;
    }

public:
    BitOutput(ostream& output) : out(output), buffer(0), count(0) {};

    inline void writeBit(int bit) {
        if (bit) {
            buffer |= (1L << count);
        }
        count++;
        if (count == 64) {
            flushBuffer();
        }
    }

    inline void flush() {
        flushBuffer();
        out.flush();
    }
};

}

#endif /* BITOUTPUT_H_ */

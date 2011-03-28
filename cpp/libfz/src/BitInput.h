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
 * BitInput.h
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#ifndef BITINPUT_H_
#define BITINPUT_H_

namespace femtozip {

class BitInput {
private:
    const char *currIn;
    const char *end;
    char buffer;
    int count;

public:
    BitInput(const char *buf, int length) : currIn(buf), end(buf + length), buffer(0), count(0) {};

    // 0, 1, or -1 for eof
    inline int readBit() {
        if (count == 0) {
            if (currIn == end) {
                // eof;
                return -1;
            }
            buffer = *currIn;
            currIn++;
            count = 8;
        }
        int bit = buffer & 1;
        buffer >>= 1;
        count--;
        return bit;
    }

};

}

#endif /* BITINPUT_H_ */

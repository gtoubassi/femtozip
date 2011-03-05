/*
 * BitInput.h
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#ifndef BITINPUT_H_
#define BITINPUT_H_

#include <iostream>

using namespace std;

namespace femtozip {

class BitInput {
private:
    istream& in;
    char buffer;
    int count;

public:
    BitInput(istream& input) : in(input), buffer(0), count(0) {};

    // 0, 1, or -1 for eof
    int readBit() {
        if (count == 0) {
            // Should I check eof before I call get?
            in.get(buffer);
            if (in.eof()) {
                return -1;
            }
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

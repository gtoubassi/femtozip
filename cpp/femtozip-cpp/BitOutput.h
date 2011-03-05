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
    char buffer;
    int count;


public:
    BitOutput(ostream& output) : out(output), buffer(0), count(0) {};

    void writeBit(int bit) {
        if (bit) {
            buffer |= (1 << count);
        }
        count++;
        if (count == 8) {
            out.put(buffer);
            buffer = 0;
            count = 0;
        }
    }

    void flush() {
        if (count > 0) {
            out.put(buffer);
            buffer = 0;
            count = 0;
        }
        out.flush();
    }
};

}

#endif /* BITOUTPUT_H_ */

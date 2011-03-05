/*
 * VerboseStringConsumer.h
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#ifndef VERBOSESTRINGCONSUMER_H_
#define VERBOSESTRINGCONSUMER_H_

#include <string>

#include "SubstringPacker.h"

using namespace std;

namespace femtozip {

class VerboseStringConsumer : public SubstringPacker::Consumer {
private:
    string output;

public:
    VerboseStringConsumer();
    ~VerboseStringConsumer();

    void encodeLiteral(int aByte);
    void encodeSubstring(int offset, int length);
    void endEncoding();

    string getOutput();
};

}

#endif /* VERBOSESTRINGCONSUMER_H_ */

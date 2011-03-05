/*
 * VerboseStringConsumer.cpp
 *
 *  Created on: Mar 2, 2011
 *      Author: gtoubassi
 */

#include <sstream>

#include "VerboseStringConsumer.h"

using namespace std;

namespace femtozip {

VerboseStringConsumer::VerboseStringConsumer() {
}

VerboseStringConsumer::~VerboseStringConsumer() {
}

void VerboseStringConsumer::encodeLiteral(int aByte) {
    output.append(1, (char)aByte);
}

void VerboseStringConsumer::encodeSubstring(int offset, int length) {
    std::stringstream out;
    out << "<" << offset << "," << length << ">";
    output += out.str();
}

void VerboseStringConsumer::endEncoding() {
}

string VerboseStringConsumer::getOutput() {
    return output;
}

}

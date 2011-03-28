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

void VerboseStringConsumer::encodeLiteral(int aByte, void *context) {
    output.append(1, (char)aByte);
}

void VerboseStringConsumer::encodeSubstring(int offset, int length, void *context) {
    std::stringstream out;
    out << "<" << offset << "," << length << ">";
    output += out.str();
}

void VerboseStringConsumer::endEncoding(void *context) {
}

string VerboseStringConsumer::getOutput() {
    return output;
}

}

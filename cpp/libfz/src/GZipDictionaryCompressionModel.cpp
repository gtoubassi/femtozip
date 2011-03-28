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

#include "GZipDictionaryCompressionModel.h"

namespace femtozip {

GZipDictionaryCompressionModel::GZipDictionaryCompressionModel() {

}

GZipDictionaryCompressionModel::~GZipDictionaryCompressionModel() {
}

static const int MaxDictLength = (1 << 15) - 1;

void GZipDictionaryCompressionModel::setDictionary(const char *dictionary, int length) {
    if (length > MaxDictLength) {
        dictionary += length - MaxDictLength;
        length = MaxDictLength;
    }
    GZipCompressionModel::setDictionary(dictionary, length);
}


bool GZipDictionaryCompressionModel::useDict() {
    return true;
}


}

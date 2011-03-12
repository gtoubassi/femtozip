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
 * CStringDocumentList.cpp
 *
 *  Created on: Feb 28, 2011
 *      Author: gtoubassi
 */

#include <iostream>
#include <cstdarg>
#include "CStringDocumentList.h"

using namespace std;

namespace femtozip {

CStringDocumentList::CStringDocumentList(vector<const char *> strs) {
    strings = strs;
}

CStringDocumentList::CStringDocumentList(const char *str1, ...) {
    strings.push_back(str1);

    va_list ap;
    va_start(ap, str1);
    const char *str;
    while ((str = va_arg(ap, const char *)) != 0) {
        strings.push_back(str);
    }
    va_end(ap);
}

CStringDocumentList::~CStringDocumentList() {
}

int CStringDocumentList::size() {
    return strings.size();
}

const char *CStringDocumentList::get(int i, int& length) {
    const char *str = strings[i];
    length = strlen(str);
    return str;
}

void CStringDocumentList::release(const char *buf) {
}


}

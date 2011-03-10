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
 * CStringDocumentList.h
 *
 *  Created on: Feb 28, 2011
 *      Author: gtoubassi
 */

#ifndef CSTRINGDOCUMENTLIST_H_
#define CSTRINGDOCUMENTLIST_H_

#include <vector>
#include "DocumentList.h"

using namespace std;

namespace femtozip {

class CStringDocumentList: public femtozip::DocumentList {

protected:
    vector<const char *> strings;

public:
    explicit CStringDocumentList(vector<const char *> strs);
    explicit CStringDocumentList(const char *str1, ...);

    virtual ~CStringDocumentList();

    int size();
    const char *get(int i, int& length);
};

}

#endif /* CSTRINGDOCUMENTLIST_H_ */

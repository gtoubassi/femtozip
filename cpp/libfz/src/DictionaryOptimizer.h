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
 * DictionaryOptimizer.h
 *
 *  Created on: Feb 28, 2011
 *      Author: gtoubassi
 */

#ifndef DICTIONARYOPTIMIZER_H_
#define DICTIONARYOPTIMIZER_H_

#include <vector>
#include "Substring.h"

using namespace std;

namespace femtozip {

class DocumentList;

class DictionaryOptimizer {

protected:
    vector<char> bytes;
    vector<int> suffixArray;
    vector<int> starts;
    int *lcpArray;
    vector<Substring> substrings;

    void computeSubstrings();
    string pack(int desiredLength);
    bool findSubstring(Substring& needle, Substring& haystack);
    int prepend(char *from, char *toStart, char *to, char *toEnd, int length);
    int docStartForIndex(int index);

public:
    explicit DictionaryOptimizer(DocumentList& documents);
    virtual ~DictionaryOptimizer();

    string optimize(int desiredLength);

    void dumpSuffixArray();
    void dumpSubstrings();
};

}

#endif /* DICTIONARYOPTIMIZER_H_ */

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

public:
    explicit DictionaryOptimizer(DocumentList& documents);
    virtual ~DictionaryOptimizer();

    string optimize(int desiredLength);

    void dumpSuffixArray();
    void dumpSubstrings();
};

}

#endif /* DICTIONARYOPTIMIZER_H_ */

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
 * DictionaryOptimizer.cpp
 *
 *  Created on: Feb 28, 2011
 *      Author: gtoubassi
 */

#include <iostream>
#include <iomanip>
#include <set>
#include <algorithm>
#include "DictionaryOptimizer.h"
#include "DocumentList.h"
#include "Substring.h"
#include "sarray.h"

using namespace std;

namespace femtozip {

DictionaryOptimizer::DictionaryOptimizer(DocumentList& documents) {
    for (int i = 0; i < documents.size(); i++) {
        int length;
        const char * buf = documents.get(i, length);
        if (length > 0) {
            int currSize = bytes.size();
            starts.push_back(currSize);
            bytes.resize(bytes.size() + length);
            memcpy(&bytes[currSize], buf, length);
        }
        delete[] buf;
    }
}

DictionaryOptimizer::~DictionaryOptimizer() {
    free(lcpArray);
}

string DictionaryOptimizer::optimize(int desiredLength) {
    suffixArray.resize(bytes.size() + 1);
    bsarray(reinterpret_cast<const uchar *>(&bytes[0]), &suffixArray[0], bytes.size());

    // Due to the extra terminator symbol that bsarray puts, suffixArray is actually 1 bigger, and bytes
    // needs to allow access to a character in the terminator's position
    bytes.push_back('\0');
    lcpArray = lcp(&suffixArray[0], &bytes[0], bytes.size());
    bytes.pop_back();

    computeSubstrings();
    return pack(desiredLength);
}

void DictionaryOptimizer::computeSubstrings() {
    vector<Substring> activeSubstrings;
    set<int> uniqueDocIds;

    int n = suffixArray.size(); // Same as lcp size

    int lastLCP = lcpArray[0];
    for (int i = 1; i <= n; i++) {
        // Note we need to process currently existing runs, so we do that by acting like we hit an LCP of 0 at the end.
        // That is why the we loop i <= n vs i < n.  Otherwise runs that exist at the end of the suffixarray/lcp will
        // never be "cashed in" and counted in the substrings.  DictionaryOptimizerTest has a unit test for this.
        int currentLCP = i == n ? 0 : lcpArray[i];

        if (currentLCP > lastLCP) {
            // The order here is important so we can optimize adding redundant strings below.
            for (int j = lastLCP + 1; j <= currentLCP; j++) {
                activeSubstrings.push_back(Substring(i, j, 0));
            }
        }
        else if (currentLCP < lastLCP) {
            int lastActiveIndex = -1, lastActiveLength = -1, lastActiveCount = -1;
            for (int j = activeSubstrings.size() - 1; j >= 0; j--) {
                if (activeSubstrings[j].getLength() > currentLCP) {
                    int activeCount = i - activeSubstrings[j].getIndex() + 1;
                    int activeLength = activeSubstrings[j].getLength();
                    int activeIndex = activeSubstrings[j].getIndex();

                    int scoreCount = activeCount;

                    // Ok we have a string which occurs activeCount times.  The true measure of its
                    // value is how many unique documents it occurs in, because occurring 1000 times in the same
                    // document isn't valuable because once it occurs once, subsequent occurrences will reference
                    // a previous occurring instance in the document.  So for 2 documents: "garrick garrick garrick toubassi",
                    // "toubassi", the string toubassi is far more valuable in a shared dictionary.  So find out
                    // how many unique documents this string occurs in.  We do this by taking the start position of
                    // each occurrence, and then map that back to the document using the "starts" array, and uniquing.
                    for (int k = activeSubstrings[j].getIndex() - 1; k < i; k++) {
                        int byteIndex = suffixArray[k];

                        // Could make this a lookup table if we are willing to burn an int[bytes.size()] but thats a lot
                        vector<int>::iterator docStart = lower_bound(starts.begin(), starts.end(), byteIndex);

                        if (docStart == starts.end() || *docStart != byteIndex) {
                            docStart--;
                        }
                        int docIndex = *docStart;

                        // While we are at it lets make sure this is a string that actually exists in a single
                        // document, vs spanning two concatenanted documents.  The idea is that for documents
                        // "http://espn.com", "http://google.com", "http://yahoo.com", we don't want to consider
                        // ".comhttp://" to be a legal string.  So make sure the length of this string doesn't
                        // cross a document boundary for this particular occurrence.
                        int nextDocStart = static_cast<unsigned int>(docIndex) < starts.size() - 1 ? starts[docIndex + 1] : bytes.size();
                        if (activeLength <= nextDocStart - byteIndex) {
                            uniqueDocIds.insert(docIndex);
                        }
                    }

                    scoreCount = uniqueDocIds.size();
                    uniqueDocIds.clear();

                    activeSubstrings.erase(activeSubstrings.begin() + j);

                    if (scoreCount == 0) {
                        continue;
                    }

                    // Don't add redundant strings.  If we just  added ABC, don't add AB if it has the same count.  This cuts down the size of substrings
                    // from growing very large.
                    if (!(lastActiveIndex != -1 && lastActiveIndex == activeIndex && lastActiveCount == activeCount && lastActiveLength > activeLength)) {

                        if (activeLength > 3) {
                            substrings.push_back(Substring(activeIndex, activeLength, scoreCount));
                        }
                    }
                    lastActiveIndex = activeIndex;
                    lastActiveLength = activeLength;
                    lastActiveCount = activeCount;
                }
            }
        }
        lastLCP = currentLCP;
    }

    sort(substrings.begin(), substrings.end()); // Likely faster with radix sort
}

string DictionaryOptimizer::pack(int desiredLength) {
    vector<Substring> pruned;
    int size = 0;

    for (int i = substrings.size() - 1; i >= 0; i--) {
        bool alreadyCovered = false;
        for (int j = 0, c = pruned.size(); j < c; j++) {
            if (findSubstring(substrings[i], pruned[j])) {
                alreadyCovered = true;
                break;
            }
        }

        if (alreadyCovered) {
            continue;
        }

        for (int j = pruned.size() - 1; j >= 0; j--) {
            if (findSubstring(pruned[j], substrings[i])) {
                size -= pruned[j].getLength();
                pruned.erase(pruned.begin() + j);
            }
        }
        pruned.push_back(substrings[i]);
        size += substrings[i].getLength();
        // We calculate 2x because when we lay the strings out end to end we will merge common prefix/suffixes
        if (size >= 2*desiredLength) {
            break;
        }
    }

    char *packed = new char[desiredLength];
    memset(packed, 0, desiredLength);
    int pi = desiredLength;

    int i, count;
    for (i = 0, count = pruned.size(); i < count && pi > 0; i++) {
        int length = pruned[i].getLength();
        if (pi - length < 0) {
            length = pi;
        }
        pi -= prepend(&bytes[0] + suffixArray[pruned[i].getIndex()], packed, packed + pi, packed + desiredLength, length);
    }

    string packedString(packed + pi, desiredLength - pi);
    delete[] packed;

    return packedString;
}

bool DictionaryOptimizer::findSubstring(Substring& needle, Substring& haystack) {

    vector<char>::iterator haystackFirst = bytes.begin() + suffixArray[haystack.getIndex()];
    vector<char>::iterator haystackLast = bytes.begin() + suffixArray[haystack.getIndex()] + haystack.getLength();
    vector<char>::iterator needleFirst = bytes.begin() + suffixArray[needle.getIndex()];
    vector<char>::iterator needleLast = bytes.begin() + suffixArray[needle.getIndex()] + needle.getLength();

    return search(haystackFirst, haystackLast, needleFirst, needleLast) != haystackLast ? true : false;
}

int DictionaryOptimizer::prepend(char *from, char *toStart, char *to, char *toEnd, int length) {
    int l;
    // See if we have a common suffix/prefix between the string being merged in, and the current strings in the front
    // of the destination.  For example if we pack " the " and then pack " and ", we should end up with " and the ", not " and  the ".
    for (l = min(length - 1, (int)(toEnd - to)); l > 0; l--) {
        if (equal(from + length - l, from + length, to)) {
            break;
        }
    }

    memcpy(to - length + l, from, length - l);
    return length - l;
}


void DictionaryOptimizer::dumpSuffixArray() {
    vector<int>::iterator i;
    for (i = suffixArray.begin(); i != suffixArray.end(); i++) {
        cout << setw(5) << *i << " ";
        cout << setw(5) << lcpArray[i - suffixArray.begin()] << " ";
        cout << "'";
        vector<char>::iterator ch;
        for (ch = bytes.begin() + *i; ch < bytes.begin() + 30 && ch != bytes.end(); ch++) {
            cout << *ch;
        }
        cout << "'" << endl;
    }
}

void DictionaryOptimizer::dumpSubstrings() {
    vector<Substring>::iterator i;
    cout << substrings.size() << " Substrings:" << endl;
    for (i = substrings.begin(); i != substrings.end(); i++) {
        cout << setw(10) << i->getScore() << " ";
        cout << "'";
        vector<char>::iterator ch = bytes.begin() + suffixArray[i->getIndex()];
        vector<char>::iterator chend = ch + i->getLength();
        for (; ch < chend; ch++) {
            cout << *ch;
        }
        cout << "'" << endl;
    }
}


}















































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
#include <string.h>
#include "DictionaryOptimizer.h"
#include "DocumentList.h"
#include "Substring.h"
#include "sarray.h"
#include "Util.h"
#include "IntSet.h"

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
        documents.release(buf);
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

    string packed = pack(desiredLength);

    return packed;
}


void DictionaryOptimizer::computeSubstrings() {
    vector<Substring> activeSubstrings;
    IntSet uniqueDocIds;
    int recentDocStartsBase;
    vector<pair<int, int> > recentDocStarts;

    int n = suffixArray.size(); // Same as lcp size

    recentDocStartsBase = 0;
    recentDocStarts.push_back(docStartForIndex(0));

    int lastLCP = lcpArray[0];
    for (int i = 1; i <= n; i++) {
        // Note we need to process currently existing runs, so we do that by acting like we hit an LCP of 0 at the end.
        // That is why the we loop i <= n vs i < n.  Otherwise runs that exist at the end of the suffixarray/lcp will
        // never be "cashed in" and counted in the substrings.  DictionaryOptimizerTest has a unit test for this.
        int currentLCP;
        if (i == n) {
            currentLCP = 0;
        }
        else {
            currentLCP = lcpArray[i];
            recentDocStarts.push_back(docStartForIndex(i));
        }

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
                        pair<int, int> docRange = recentDocStarts[k - recentDocStartsBase];

                        // While we are at it lets make sure this is a string that actually exists in a single
                        // document, vs spanning two concatenated documents.  The idea is that for documents
                        // "http://espn.com", "http://google.com", "http://yahoo.com", we don't want to consider
                        // ".comhttp://" to be a legal string.  So make sure the length of this string doesn't
                        // cross a document boundary for this particular occurrence.
                        if (activeLength <= docRange.second - (byteIndex - docRange.first)) {
                            uniqueDocIds.put(docRange.first);
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

                        // Empirically determined that we need 4 chars for it to be worthwhile.  Note gzip takes 3, so cause for skepticism at going with 4.
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

        if (activeSubstrings.size() == 0 && recentDocStarts.size() > 1) {
            pair<int, int> last = *(recentDocStarts.end() - 1);
            recentDocStartsBase += recentDocStarts.size() - 1;
            recentDocStarts.clear();
            recentDocStarts.push_back(last);
        }
    }

    sort(substrings.begin(), substrings.end());
}

string DictionaryOptimizer::pack(int desiredLength) {


    // First, filter out the substrings to remove overlap since
    // many of the substrings are themselves substrings of each other (e.g. 'http://', 'ttp://').

    vector<Substring> pruned;
    int size = 0;

    for (int i = substrings.size() - 1; i >= 0; i--) {

        // Is this substring already covered within the pruned list?
        // e.g. if we are considering "ttp://" when "http://" has been selected.
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

        // If this is a superstring of already included strings, this will subsume them
        // e.g. we are adding "rosebuds" but have previously added "rose" and "buds"

        for (int j = pruned.size() - 1; j >= 0; j--) {
            if (findSubstring(pruned[j], substrings[i])) {
                size -= pruned[j].getLength();
                pruned.erase(pruned.begin() + j);
            }
        }
        pruned.push_back(substrings[i]);
        size += substrings[i].getLength();
        if (size >= desiredLength) {
            break;
        }
    }

    // Now pack the substrings end to end, taking advantage of potential prefix/suffix overlap
    // (e.g. if we are packing "toubassi" and "silence", pack it as
    // "toubassilence" vs "toubassisilence")

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

    //cout << packedString << endl;

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

/**
 * Returns the offset into the byte buffer representing the
 * start of the document which contains the specified byte
 * (as an offset into the byte buffer).  So for example
 * docStartForIndex(0) always returns 0, and
 * docStartForIndex(15) will return 10 if the first doc is
 * 10 bytes and the second doc is at least 5.
 */
pair<int, int> DictionaryOptimizer::docStartForIndex(int index) {
    int byteIndex = suffixArray[index];
    vector<int>::iterator docStart = lower_bound(starts.begin(), starts.end(), byteIndex);
    if (docStart == starts.end() || *docStart != byteIndex) {
        docStart--;
    }
    int nextDoc;
    if (docStart == (starts.end() - 1)) {
        nextDoc = bytes.size();
    }
    else {
        nextDoc = *(docStart + 1);
    }
    return pair<int, int>(*docStart, nextDoc - (*docStart));
}


void DictionaryOptimizer::dumpSuffixArray() {
    cout << string(&bytes[0], bytes.size()) << endl;
    for (size_t i = 0; i < suffixArray.size(); i++) {
        cout << right << setw(6) << i << setw(4) << suffixArray[i] << setw(4) << lcpArray[i] << left << "   " << string(&bytes[suffixArray[i]], bytes.size() - suffixArray[i]) << endl;
    }
}

void DictionaryOptimizer::dumpSubstrings(vector<Substring>& subs) {
    vector<Substring>::reverse_iterator i;
    int index = 0;
    for (i = subs.rbegin(); i != subs.rend(); i++, index++) {
        cout << right << setw(6) << index << setw(4) << i->getScore() << left << "   " << string(&bytes[suffixArray[i->getIndex()]], i->getLength()) << endl;
    }
}


void DictionaryOptimizer::dumpSubstrings() {
    dumpSubstrings(substrings);
}

}



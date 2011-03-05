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
package org.toubassi.femtozip.dictionary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.toubassi.femtozip.DocumentList;


public class DictionaryOptimizer {

    private SubstringArray substrings;
    private byte[] bytes;
    private int[] suffixArray;
    private int[] lcp;
    private int[] starts;
    
    public DictionaryOptimizer(DocumentList documents) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        starts = new int[documents.size()];
        
        for (int i = 0, count = documents.size(); i < count; i++) {
            byte[] document = documents.get(i);
            starts[i] = bytesOut.size();
            bytesOut.write(document);
        }
        
        bytes = bytesOut.toByteArray();
    }
    
    public byte[] optimize(int desiredLength) {
        suffixArray = SuffixArray.computeSuffixArray(bytes);
        lcp = SuffixArray.computeLCP(bytes, suffixArray);
        computeSubstrings();
        return pack(desiredLength);
    }

    protected void computeSubstrings() {
        SubstringArray activeSubstrings = new SubstringArray(128);
        Set<Integer> uniqueDocIds = new HashSet<Integer>();
        
        substrings = new SubstringArray(1024);
        int n = lcp.length;
        
        int lastLCP = lcp[0];
        for (int i = 1; i < n; i++) {
            int currentLCP = lcp[i];
            
            if (currentLCP > lastLCP) {
                // The order here is important so we can optimize adding redundant strings below.
                for (int j = lastLCP + 1; j <= currentLCP; j++) {
                    activeSubstrings.add(i, j, 0);
                }
            }
            else if (currentLCP < lastLCP) {
                int lastActiveIndex = -1, lastActiveLength = -1, lastActiveCount = -1;
                for (int j = activeSubstrings.size() - 1; j >= 0; j--) {
                    if (activeSubstrings.length(j) > currentLCP) {
                        int activeCount = i - activeSubstrings.index(j) + 1;
                        int activeLength = activeSubstrings.length(j);
                        int activeIndex = activeSubstrings.index(j);

                        int scoreCount = activeCount;

                        // Ok we have a string which occurs activeCount times.  The true measure of its
                        // value is how many unique documents it occurs in, because occurring 1000 times in the same
                        // document isn't valuable because once it occurs once, subsequent occurrences will reference
                        // a previous occurring instance in the document.  So for 2 documents: "garrick garrick garrick toubassi",
                        // "toubassi", the string toubassi is far more valuable in a shared dictionary.  So find out
                        // how many unique documents this string occurs in.  We do this by taking the start position of
                        // each occurrence, and then map that back to the document using the "starts" array, and uniquing.
                        for (int k = activeSubstrings.index(j) - 1; k < i; k++) {
                            int byteIndex = suffixArray[k];
                            
                            // Could make this a lookup table if we are willing to burn an int[bytes.length] but thats a lot
                            int docIndex = Arrays.binarySearch(starts, byteIndex);
                            
                            if (docIndex < 0) {
                                docIndex = -docIndex - 2;
                            }
                            
                            // While we are at it lets make sure this is a string that actually exists in a single
                            // document, vs spanning two concatenanted documents.  The idea is that for documents
                            // "http://espn.com", "http://google.com", "http://yahoo.com", we don't want to consider
                            // ".comhttp://" to be a legal string.  So make sure the length of this string doesn't
                            // cross a document boundary for this particular occurrence.
                            int nextDocStart = docIndex < starts.length - 1 ? starts[docIndex + 1] : bytes.length;
                            if (activeLength <= nextDocStart - byteIndex) {
                                uniqueDocIds.add(docIndex);
                            }
                        }
                        
                        scoreCount = uniqueDocIds.size();
                        uniqueDocIds.clear();

                        activeSubstrings.remove(j);
                        
                        if (scoreCount == 0) {
                            continue;
                        }
                        
                        // Don't add redundant strings.  If we just  added ABC, don't add AB if it has the same count.  This cuts down the size of substrings
                        // from growing very large.
                        if (!(lastActiveIndex != -1 && lastActiveIndex == activeIndex && lastActiveCount == activeCount && lastActiveLength > activeLength)) {

                            if (activeLength > 3) {
                                substrings.add(activeIndex, activeLength, scoreCount);
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
        substrings.sort();
    }
    
    protected byte[] pack(int desiredLength) {
        SubstringArray pruned = new SubstringArray(1024);
        int size = 0;
        
        for (int i = substrings.size() - 1; i >= 0; i--) {
            boolean alreadyCovered = false;
            for (int j = 0, c = pruned.size(); j < c; j++) {
                if (pruned.indexOf(j, substrings, i, bytes, suffixArray) != -1) {
                    
                    alreadyCovered = true;
                    break;
                }
            }
            
            if (alreadyCovered) {
                continue;
            }
            
            for (int j = pruned.size() - 1; j >= 0; j--) {
                if (substrings.indexOf(i, pruned, j, bytes, suffixArray) != -1) {
                    size -= pruned.length(j);
                    pruned.remove(j);
                }
            }
            pruned.setScore(pruned.size(), substrings.index(i), substrings.length(i), substrings.score(i));
            size += substrings.length(i);
            // We calculate 2x because when we lay the strings out end to end we will merge common prefix/suffixes
            if (size >= 2*desiredLength) {
                break;
            }
        }

        byte[] packed = new byte[desiredLength];
        int pi = desiredLength;
        
        int i, count;
        for (i = 0, count = pruned.size(); i < count && pi > 0; i++) {
            int length = pruned.length(i);
            if (pi - length < 0) {
                length = pi;
            }
            pi -= prepend(bytes, suffixArray[pruned.index(i)], packed, pi, length);
        }
        
        return packed;
    }
    
    protected int prepend(byte[] from, int fromIndex, byte[] to, int toIndex, int length) {
        int l;
        // See if we have a common suffix/prefix between the string being merged in, and the current strings in the front
        // of the destination.  For example if we pack " the " and then pack " and ", we should end up with " and the ", not " and  the ".
        for (l = Math.min(length - 1, to.length - toIndex); l > 0; l--) {
            if (byteRangeEquals(from, fromIndex + length - l, to, toIndex, l)) {
                break;
            }
        }
        
        System.arraycopy(from, fromIndex, to, toIndex - length + l, length - l);
        return length - l;
    }
    
    private static boolean byteRangeEquals(byte[] bytes1, int index1, byte[] bytes2, int index2, int length) {
        
        for (;length > 0; length--, index1++, index2++) {
            if (bytes1[index1] != bytes2[index2]) {
                return false;
            }
        }
        return true;
    }
    
    public int getSubstringCount() {
        return substrings.size();
    }
    
    public int getSubstringScore(int i) {
        return substrings.score(i);
    }
    
    public byte[] getSubstringBytes(int i) {
        int index = suffixArray[substrings.index(i)];
        int length = substrings.length(i);
        return Arrays.copyOfRange(bytes, index, index + length);
    }
    
    /**
     * For debugging
     */
    public void dumpSubstrings(PrintStream out) {
        if (substrings != null) {
            for (int j = substrings.size() - 1; j >= 0; j--) {
                out.print(substrings.score(j) + "\t");
                out.write(bytes, suffixArray[substrings.index(j)], Math.min(40, substrings.length(j)));
                out.println();
            }
        }
    }
}

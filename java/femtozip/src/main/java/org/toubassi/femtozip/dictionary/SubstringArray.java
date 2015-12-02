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

import java.util.Arrays;

public class SubstringArray {
    
    private int[] indexes;
    private int[] lengths;
    private int[] scores;
    private int size;
    private int capacity;
    
    public SubstringArray(int capacity) {
        this.capacity = capacity;
        indexes = new int[capacity];
        lengths = new int[capacity];
        scores = new int[capacity];
    }
    
    public void sort() {
        int[] histogram = new int[256];
        SubstringArray working = new SubstringArray(size);
         
        for (int bitOffset = 0; bitOffset <= 24; bitOffset += 8) {

            if (bitOffset > 0) {
                Arrays.fill(histogram, 0);
            }
            
            for (int i = 0, count = size; i < count; i++) {
                int sortValue = scores[i];
                int sortByte = (sortValue >> bitOffset) & 0xff;
                histogram[sortByte]++;
            }
            
            for (int i = 0, count = histogram.length, rollingSum = 0; i < count; i++) {
                int t = histogram[i];
                histogram[i] = rollingSum;
                rollingSum += t;
            }
            
            for (int i = 0, count = size; i < count; i++) {
                int sortValue = scores[i];
                int sortByte = (sortValue >> bitOffset) & 0xff;
                int newOffset = histogram[sortByte]++;
                working.setScore(newOffset, indexes[i], lengths[i], this.scores[i]);
            }

            // swap (brain transplant) innards
            Object t = working.indexes;
            working.indexes = indexes;
            indexes = (int[])t;
            
            t = working.lengths;
            working.lengths = lengths;
            lengths = (int[])t;
            
            t = working.scores;
            working.scores = scores;
            scores = (int[])t;
            
            size = working.size;
            working.size = 0;
            
            int i = working.capacity;
            working.capacity = capacity;
            capacity = i;
        }
    }
    
    
    public int add(int index, int length, int count) {
        return setScore(size, index, length, computeScore(length, count));
    }
    
    public int setScore(int i, int index, int length, int score) {
        if (i >= capacity) {
            int growBy = (((i - capacity) / (8*1024)) + 1) * 8*1024;
            
            // Since this array is going to be VERY big, don't double.            
            int[] newindex = new int[this.indexes.length + growBy];
            System.arraycopy(this.indexes, 0, newindex, 0, this.indexes.length);
            this.indexes = newindex;

            int[] newlength = new int[this.lengths.length + growBy];
            System.arraycopy(this.lengths, 0, newlength, 0, this.lengths.length);
            this.lengths = newlength;
            
            int[] newscores = new int[this.scores.length + growBy];
            System.arraycopy(this.scores, 0, newscores, 0, this.scores.length);
            this.scores = newscores;
            
            capacity = this.indexes.length;
        }
        
        this.indexes[i] = index;
        this.lengths[i] = length;
        this.scores[i] = score;
        
        size = Math.max(i + 1, size);
        
        return i;
    }
    
    public void remove(int i) {
        System.arraycopy(indexes, i + 1, indexes, i, size - i - 1);
        System.arraycopy(lengths, i + 1, lengths, i, size - i - 1);
        System.arraycopy(scores, i + 1, scores, i, size - i - 1);
        size--;
    }
    
    public int size() {
        return size;
    }
    
    public int index(int i) {
        return indexes[i];
    }
    
    public int length(int i) {
        return lengths[i];
    }

    public int score(int i) {
        return scores[i];
    }

    public int indexOf(int s1, SubstringArray sa, int s2, byte[] s, int[] prefixes) {
        int index1 = indexes[s1];
        int length1 = lengths[s1];
        int index2 = sa.indexes[s2];
        int length2 = sa.lengths[s2];
        
        for (int i = prefixes[index1], n = prefixes[index1] + length1 - length2 + 1; i < n; i++) {
            boolean found = true;
            for (int j = prefixes[index2], nj = prefixes[index2] + length2, i1 = i; j < nj; j++, i1++) {
                if (s[i1] != s[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    /*
     * Substring of length n occurring m times.  We will reduce output by n*m characters, and add 3*m offsets/lengths.  So net benefit is (n - 3)*m.
     * Costs n characters to include in the compression dictionary, so compute a "per character consumed in the compression dictionary" benefit.
     * score = m*(n-3)/n
     */
    private int computeScore(int length, int count) {
        if (length <= 3) {
            return 0;
        }
        return (100 * count * (length - 3)) / length;
    }
}

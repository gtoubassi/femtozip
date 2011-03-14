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
package org.toubassi.femtozip.substring;

import java.util.Arrays;

public class PrefixHash {
    
    public static final int PrefixLength = 4;
    
    private byte[] buffer;
    int[] hash;
    int[] heap;
    
    public PrefixHash(byte[] buf, boolean addToHash) {
        buffer = buf;
        hash = new int[(int)(1.5 * buf.length)];
        Arrays.fill(hash, -1);
        heap = new int[buf.length];
        Arrays.fill(heap, -1);
        if (addToHash) {
            for (int i = 0, count = buf.length - PrefixLength; i < count; i++) {
                put(i);
            }
        }
    }
    
    private int hashIndex(byte[] buf, int i) {
        int code = buf[i] | (buf[i + 1] << 8) | (buf[i + 2] << 16) | (buf[i + 3] << 24);
        return code % hash.length;
    }

    
    public void put(int index) {
        int hashIndex = hashIndex(buffer, index);
        heap[index] = hash[hashIndex];
        hash[hashIndex] = index;
    }

    public final void getBestMatch(int index, byte[] targetBuf, int[] bestMatchIndex, int[] bestMatchLength) {
        bestMatchIndex[0] = 0;
        bestMatchLength[0] = 0;
        
        byte[] buf = buffer;
        int bufLen = buf.length;
        
        if (bufLen == 0) {
            return;
        }
        
        int targetBufLen = targetBuf.length;

        int targetHashIndex = hashIndex(targetBuf, index);
        int candidateIndex = hash[targetHashIndex];
        while (candidateIndex != -1) {
            int distance;
            if (targetBuf != buf) {
                distance = index + bufLen - candidateIndex;
            }
            else {
                distance = index - candidateIndex;
            }
            if (distance > (2<<15)-1) {
                // Since we are iterating over nearest offsets first, once we pass 64k
                // we know the rest are over 64k too.
                break;
            }

            int maxMatchJ = Math.min(index + 255, targetBufLen);
            int maxMatchK = Math.min(candidateIndex + 255, bufLen);
            int j, k;
            for (j = index, k = candidateIndex; j < maxMatchJ && k < maxMatchK; j++, k++) {
                if (buf[k] != targetBuf[j]) {
                    break;
                }
            }
            
            int matchLength = j - index;
            if (matchLength > bestMatchLength[0]) {
                bestMatchIndex[0] = candidateIndex;
                bestMatchLength[0] = matchLength;
            }
            candidateIndex = heap[candidateIndex];
        }
    }
    
}

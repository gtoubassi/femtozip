package org.toubassi.femtozip.substring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PrefixHash {
    
    private byte[] buffer;
    private HashMap<Trigraph, List<Integer>> hash;
    
    public PrefixHash(byte[] buf, boolean addToHash) {
        buffer = buf;
        hash  = new HashMap<Trigraph, List<Integer>>(buf.length);
        if (addToHash) {
            for (int i = 0, count = buf.length - 3; i < count; i++) {
                put(i);
            }
        }
    }
    
    public void put(int index) {
        Trigraph trigraph = new Trigraph(buffer[index], buffer[index + 1], buffer[index + 2]);
        List<Integer> list = hash.get(trigraph);
        if (list == null) {
            list = new ArrayList<Integer>();
            hash.put(trigraph, list);
        }
        list.add(index);
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

        Trigraph trigraph = new Trigraph(targetBuf[index], targetBuf[index + 1], targetBuf[index + 2]);
        List<Integer> list = hash.get(trigraph);
        if (list == null) {
            return;
        }
        
        for (int i = list.size() - 1; i >= 0; i--) {
            int candidateIndex = list.get(i);
            
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
        }
    }
    
}

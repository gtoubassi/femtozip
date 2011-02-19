package org.toubassi.femtozip.dictionary;

import java.io.PrintStream;



public class DictionaryOptimizer {

    private SubstringArray substrings;
    private byte[] bytes;
    private int[] suffixArray;
    private int[] lcp;
    
    public DictionaryOptimizer(byte[] inputBytes) {
        this.bytes = inputBytes;
    }
    
    public byte[] optimize(int desiredLength) {
        suffixArray = SuffixArray.computeSuffixArray(bytes);
        lcp = SuffixArray.computeLCP(bytes, suffixArray);
        computeSubstrings();
        return pack(desiredLength);
    }

    protected void computeSubstrings() {
        SubstringArray activeSubstrings = new SubstringArray(128);
        
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
                        
                        // Don't add redundant strings.  If we just  added ABC, don't add AB if it has the same count.  This cuts down the size of substrings
                        // from growing very large.
                        if (!(lastActiveIndex != -1 && lastActiveIndex == activeIndex && lastActiveCount == activeCount && lastActiveLength > activeLength)) {

                            if (activeLength > 3) {
                                substrings.add(activeIndex, activeLength, activeCount);
                            }
                        }
                        lastActiveIndex = activeIndex;
                        lastActiveLength = activeLength;
                        lastActiveCount = activeCount;
                        activeSubstrings.remove(j);
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
            if (size >= desiredLength) {
                break;
            }
        }

        byte[] packed = new byte[desiredLength];
        int pi = desiredLength;
        
        for (int i = 0, count = pruned.size(); i < count && pi > 0; i++) {
            int length = pruned.length(i);
            pi -= length;
            if (pi < 0) {
                length += pi;
                pi = 0;
            }
            System.arraycopy(bytes, suffixArray[pruned.index(i)], packed, pi, length);
        }
        
        if (pi > 0 && size >= desiredLength) {
            System.out.println("FAIL: " + pi + " " + size + " " + desiredLength);
        }
        
        return packed;
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

package org.toubassi.femtozip.substring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SubstringPacker {
    private static final int MinimumMatchLength = 4;
    
    private byte[] dictionary;
    
    public interface Consumer {
        public void encodeLiteral(int aByte);
        public void encodeSubstring(int offset, int length);
    }
    
    public SubstringPacker(byte[] dictionary) {
        this.dictionary = dictionary == null ? new byte[0] : dictionary;
    }
    
    public void pack(byte[] rawBytes, SubstringPacker.Consumer encoder) {
        HashMap<Trigraph, ArrayList<Integer>> previousStrings = new HashMap<Trigraph, ArrayList<Integer>>();

        byte[] newRawBytes = Arrays.copyOf(dictionary, rawBytes.length + dictionary.length);
        System.arraycopy(rawBytes, 0, newRawBytes, dictionary.length, rawBytes.length);
        rawBytes = newRawBytes;
        
        for (int i = 0, count = dictionary.length - 2; i < count; i++) {
            Trigraph trigraph = new Trigraph(rawBytes[i], rawBytes[i + 1], rawBytes[i + 2]);
            addTrigraphLocation(previousStrings, trigraph, i);
        }

        
        int previousMatchIndex = 0;
        int previousMatchLength = 0;
        
        int curr, count;
        for (curr = dictionary.length, count = rawBytes.length; curr < count; curr++) {
            int bestMatchIndex = 0;
            int bestMatchLength = 0;
            
            if (curr + 2 < count) {
                Trigraph trigraph = new Trigraph(rawBytes[curr], rawBytes[curr + 1], rawBytes[curr + 2]);
                ArrayList<Integer> matches = getTrigraphLocations(previousStrings, trigraph);
                
                // find the best match
                // Always check nearest indexes first.
                for (int i = matches.size() - 1; i >= 0; i--) {
                    int index = matches.get(i);

                    // Make sure we are within 64k.  This is arbitrary, but is
                    // used in the symbol encoding stage (8 bits for match length, 16 bits for match offset)
                    if (curr - index > (2<<15)-1) {
                        // Since we are iterating over nearest offsets first, once we pass 64k
                        // we know the rest are over 64k too.
                        break;
                    }
                    
                    // We know the first 3 bytes already match since they share the same trigraph.
                    int j, k, maxMatch;
                    for (j = curr + 3, k = index + 3, maxMatch = Math.min(curr + 255, rawBytes.length); j < maxMatch; j++, k++) {
                        if (rawBytes[j] != rawBytes[k]) {
                            break;
                        }
                    }
                    
                    int matchLength = k - index;
                    if (matchLength > bestMatchLength) {
                        bestMatchIndex = index;
                        bestMatchLength = matchLength;
                    }
                }
                
                matches.add(curr);
            }
            
            if (bestMatchLength < MinimumMatchLength) {
                bestMatchIndex = bestMatchLength = 0;
            }
            
            if (previousMatchLength > 0 && bestMatchLength <= previousMatchLength) {
                // We didn't get a match or we got one and the previous match is better
                encoder.encodeSubstring(-(curr - 1 - previousMatchIndex), previousMatchLength);
                
                // Make sure locations are added for the match.  This allows repetitions to always
                // encode the same relative locations which is better for compressing the locations.
                int endMatch = curr - 1 + previousMatchLength;
                curr++;
                while (curr < endMatch && curr + 2 < count) {
                    Trigraph t = new Trigraph(rawBytes[curr], rawBytes[curr + 1], rawBytes[curr + 2]);
                    addTrigraphLocation(previousStrings, t, curr);
                    curr++;
                }
                curr = endMatch - 1; // Make sure 'curr' is pointing to the last processed byte so it is at the right place in the next iteration
                previousMatchIndex = previousMatchLength = 0;
            }
            else if (previousMatchLength > 0 && bestMatchLength > previousMatchLength) {
                // We have a match, and we had a previous match, and this one is better.
                previousMatchIndex = bestMatchIndex;
                previousMatchLength = bestMatchLength;
                encoder.encodeLiteral(((int)rawBytes[curr - 1]) & 0xff);
            }
            else if (bestMatchLength > 0) {
                // We have a match, but no previous match
                previousMatchIndex = bestMatchIndex;
                previousMatchLength = bestMatchLength;
            }
            else if (bestMatchLength == 0 && previousMatchLength == 0) {
                // No match, and no previous match.
                encoder.encodeLiteral(((int)rawBytes[curr]) & 0xff);
            }
        }
    }

    private void addTrigraphLocation(HashMap<Trigraph, ArrayList<Integer>> table, Trigraph trigraph, int location) {
        ArrayList<Integer> matches = getTrigraphLocations(table, trigraph);
        matches.add(location);
    }
    
    private ArrayList<Integer> getTrigraphLocations(HashMap<Trigraph, ArrayList<Integer>> table, Trigraph trigraph) {
        ArrayList<Integer> matches = table.get(trigraph);
        
        if (matches == null) {
            matches = new ArrayList<Integer>();
            table.put(trigraph, matches);
        }
        return matches;
    }
}

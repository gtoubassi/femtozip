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


public class SubstringPacker {
    private static final int MinimumMatchLength = PrefixHash.PrefixLength;
    
    private byte[] dictionary;
    private PrefixHash dictHash;
    
    public interface Consumer {
        public void encodeLiteral(int aByte);
        public void encodeSubstring(int offset, int length);
        public void endEncoding();
    }
    
    public SubstringPacker(byte[] dictionary) {
        this.dictionary = dictionary = dictionary == null ? new byte[0] : dictionary;
        dictHash = new PrefixHash(dictionary, true);
    }
    
    public void pack(byte[] rawBytes, SubstringPacker.Consumer consumer) {
        PrefixHash hash = new PrefixHash(rawBytes, false);
        int dictLen = dictionary.length;

        int previousMatchIndex = 0;
        int previousMatchLength = 0;
        
        int[] matchIndex = new int[1];
        int[] matchLength = new int[1];
        
        int curr, count;
        for (curr = 0, count = rawBytes.length; curr < count; curr++) {
            int bestMatchIndex = 0;
            int bestMatchLength = 0;
            
            if (curr + PrefixHash.PrefixLength - 1 < count) {
                dictHash.getBestMatch(curr, rawBytes, matchIndex, matchLength);
                bestMatchIndex = matchIndex[0];
                bestMatchLength = matchLength[0];
                hash.getBestMatch(curr, rawBytes, matchIndex, matchLength);
                
                // Note the >= because we prefer a match that is nearer (and a match
                // in the string being compressed is always closer than one from the dict).
                if (matchLength[0] >= bestMatchLength) {
                    bestMatchIndex = matchIndex[0] + dictLen;
                    bestMatchLength = matchLength[0];
                }
                
                hash.put(curr);
            }
            
            if (bestMatchLength < MinimumMatchLength) {
                bestMatchIndex = bestMatchLength = 0;
            }
            
            if (previousMatchLength > 0 && bestMatchLength <= previousMatchLength) {
                // We didn't get a match or we got one and the previous match is better
                consumer.encodeSubstring(-(curr + dictLen - 1 - previousMatchIndex), previousMatchLength);
                
                // Make sure locations are added for the match.  This allows repetitions to always
                // encode the same relative locations which is better for compressing the locations.
                int endMatch = curr - 1 + previousMatchLength;
                curr++;
                while (curr < endMatch && curr + PrefixHash.PrefixLength < count) {
                    hash.put(curr);
                    curr++;
                }
                curr = endMatch - 1; // Make sure 'curr' is pointing to the last processed byte so it is at the right place in the next iteration
                previousMatchIndex = previousMatchLength = 0;
            }
            else if (previousMatchLength > 0 && bestMatchLength > previousMatchLength) {
                // We have a match, and we had a previous match, and this one is better.
                previousMatchIndex = bestMatchIndex;
                previousMatchLength = bestMatchLength;
                consumer.encodeLiteral(((int)rawBytes[curr - 1]) & 0xff);
            }
            else if (bestMatchLength > 0) {
                // We have a match, but no previous match
                previousMatchIndex = bestMatchIndex;
                previousMatchLength = bestMatchLength;
            }
            else if (bestMatchLength == 0 && previousMatchLength == 0) {
                // No match, and no previous match.
                consumer.encodeLiteral(((int)rawBytes[curr]) & 0xff);
            }
        }
        consumer.endEncoding();
    }

}

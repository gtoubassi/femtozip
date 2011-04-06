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
package org.toubassi.femtozip.coding.huffman;


public class DecodeTable {
    
    Codeword[] codes = new Codeword[256];
    DecodeTable[] tables = new DecodeTable[256];
    
    public void build(Codeword[] encoding) {
        build(encoding, 0);
    }
    
    private void build(Codeword[] encoding, int consumedBitLength) {
        
        for (int i = 0, count = encoding.length; i < count; i++) {
            Codeword code = encoding[i];
            if (code == null) {
                continue;
            }
            buildCodeword(consumedBitLength, code);
        }
        
    }

    private void buildCodeword(int consumedBitLength, Codeword code) {
        int activeBitLength = code.bitLength - consumedBitLength;
        
        if (activeBitLength <= 8) {
            
            for (int j = 0, jcount = 1 << (8 - activeBitLength); j < jcount; j++) {
                int index = (j << activeBitLength) | (code.value >> consumedBitLength);
                codes[index] = code;
            }
        }
        else {
            int index = (code.value >> consumedBitLength) & 0xff;
            DecodeTable subtable = tables[index];
            if (subtable == null) {
                subtable = new DecodeTable();
                tables[index] = subtable;
            }
            subtable.buildCodeword(consumedBitLength + 8, code);
        }
    }
    
    public Codeword decode(int value) {
        int index = value & 0xff;
        Codeword code = codes[index];
        if (code != null) {
            return code;
        }
        else {
            return tables[index].decode(value>>8);
        }
    }

}

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

import java.io.IOException;
import java.io.InputStream;

import com.colloquial.io.BitInput;

public class HuffmanDecoder {
    private BitInput in;
    private HuffmanModel model;
    private long bitBuf;
    private int availableBits;
    private boolean endOfStream;
    
    
    public HuffmanDecoder(HuffmanModel model, InputStream in) throws IOException {
        this.in = new BitInput(in);
        this.model = model;
    }
    
    public int decodeSymbol() throws IOException {
        if (endOfStream) {
            return -1;
        }
        while (availableBits < 32 && !in.endOfStream()) {
            if (in.readBit()) {
                bitBuf |= 1L << availableBits;
            }
            availableBits ++;
        }
        
        Codeword decoded = model.decode((int)bitBuf);
        if (model.isEOF(decoded)) {
            endOfStream = true;
            return -1;
        }
        bitBuf >>= decoded.bitLength;
        availableBits -= decoded.bitLength;
        return decoded.symbol;
    }
}

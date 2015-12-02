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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Codeword implements Cloneable {
    int value;
    int bitLength;
    int symbol;

    public Codeword() {
    }
    
    public int getSymbol() {
        return symbol;
    }

    public void load(DataInputStream in) throws IOException {
        value = in.readInt();
        bitLength = in.readInt();
        symbol = in.readInt();
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(value);
        out.writeInt(bitLength);
        out.writeInt(symbol);
    }
    
    public boolean equals(Object other) {
        Codeword o = (Codeword)other;
        return value == o.value && bitLength == o.bitLength && symbol == o.symbol;
    }
    
    public Codeword clone() {
        try {
            Codeword copy = (Codeword)super.clone();
            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void appendBit(int value) {
        this.value |= (0x1 & value) << bitLength;
        bitLength++;
    }

    public String toString() {
        String s = Integer.toString(value, 2);
        if (s.length() < bitLength) {
            while (s.length() < bitLength) {
                s = '0' + s;
            }
            return s;
        }
        return s.substring(s.length() - bitLength, bitLength);
    }
    
    public void write(BitOutput bitOut) throws IOException {
        int l = bitLength;
        int v = value;
        
        while (l > 0) {
            bitOut.writeBit((v & 0x1));
            l--;
            v >>= 1;
        }
    }

}

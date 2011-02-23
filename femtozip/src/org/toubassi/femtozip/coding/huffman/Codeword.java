package org.toubassi.femtozip.coding.huffman;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.colloquial.arithcode.BitOutput;

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
            bitOut.writeBit((v & 0x1) == 1 ? true : false);
            l--;
            v >>= 1;
        }
    }

}

package org.toubassi.femtozip.coding.huffman;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DecodeTable {
    
    Codeword[] codes = new Codeword[256];
    DecodeTable[] tables = new DecodeTable[256];
    
    public void load(DataInputStream in) throws IOException {
        for (int i = 0, count = codes.length; i < count; i++) {
            if (in.readBoolean()) {
                codes[i] = new Codeword();
                codes[i].load(in);
            }
            else {
                codes[i] = null;
            }
        }
        for (int i = 0, count = tables.length; i < count; i++) {
            if (in.readBoolean()) {
                tables[i] = new DecodeTable();
                tables[i].load(in);
            }
            else {
                tables[i] = null;
            }
        }
    }

    public void save(DataOutputStream out) throws IOException {
        for (int i = 0, count = codes.length; i < count; i++) {
            if (codes[i] != null) {
                out.writeBoolean(true);
                codes[i].save(out);
            }
            else {
                out.writeBoolean(false);
            }
        }
        for (int i = 0, count = tables.length; i < count; i++) {
            if (tables[i] != null) {
                out.writeBoolean(true);
                tables[i].save(out);
            }
            else {
                out.writeBoolean(false);
            }
        }
    }
    
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

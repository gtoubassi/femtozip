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

package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;
import java.io.InputStream;

import com.colloquial.arithcode.BitInput;

public class HuffmanDecoder {
    private BitInput in;
    private HuffmanModel model;
    private long bitBuf;
    private int availableBits;
    private Codeword eof;
    private boolean endOfStream;
    
    
    public HuffmanDecoder(HuffmanModel model, InputStream in) throws IOException {
        this.in = new BitInput(in);
        this.model = model;
        eof = model.encoding[model.encoding.length - 1];
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
        
        Codeword decoded = model.decoding.decode((int)bitBuf);
        if (decoded.value == eof.value && decoded.bitLength == eof.bitLength) {
            endOfStream = true;
            return -1;
        }
        bitBuf >>= decoded.bitLength;
        availableBits -= decoded.bitLength;
        return decoded.symbol;
    }
}

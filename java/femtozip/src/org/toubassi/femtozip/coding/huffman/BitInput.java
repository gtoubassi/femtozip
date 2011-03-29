package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;
import java.io.InputStream;

public class BitInput {
    
    private InputStream in;
    private int buffer;
    private int count;
    
    public BitInput(InputStream input) {
        in = input;
    }
    
    public int readBit() throws IOException {
        if (count == 0) {
            buffer = in.read();
            if (buffer == -1) {
                // eof
                return -1;
            }
            count = 8;
        }
        int bit = buffer & 1;
        buffer >>= 1;
        count--;
        return bit;
    }
}

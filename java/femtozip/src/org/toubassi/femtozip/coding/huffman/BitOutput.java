package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;
import java.io.OutputStream;

public class BitOutput {
    
    private OutputStream out;
    private int buffer;
    private int count;
    
    public BitOutput(OutputStream output) {
        out = output;
    }

    public void writeBit(int bit) throws IOException  {
        if (bit > 0) {
            buffer |= (1 << count);
        }
        count++;
        if (count == 8) {
            out.write(buffer);
            buffer = 0;
            count = 0;
        }
    }

    public void flush() throws IOException {
        if (count > 0) {
            out.write(buffer);
            buffer = 0;
            count = 0;
        }
    }
    
    public void close() throws IOException {
        flush();
        out.close();
    }
}

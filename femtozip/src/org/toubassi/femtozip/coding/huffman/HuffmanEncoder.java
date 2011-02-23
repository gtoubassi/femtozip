package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;
import java.io.OutputStream;

import com.colloquial.arithcode.BitOutput;

public class HuffmanEncoder {
    private BitOutput bitOut;
    private HuffmanModel model;
    
    public HuffmanEncoder(HuffmanModel model, OutputStream out) {
        bitOut = new BitOutput(out);
        this.model = model;
    }
    
    public void encodeSymbol(int symbol) throws IOException {
        model.encode(symbol).write(bitOut);
    }
    
    public void close() throws IOException {
        model.getCodewordForEOF().write(bitOut);//EOF
        // regrettable we can't flush without closing.  Yes bitoutput has flush
        // but it doesn't write the last fractional bit.  Could add that if we needed.
        bitOut.close();
    }
}

package org.toubassi.femtozip.huffman;

import java.io.IOException;
import java.io.OutputStream;

import com.colloquial.arithcode.BitOutput;

public class Encoder {
    private BitOutput bitOut;
    private HuffmanModel model;
    
    public Encoder(HuffmanModel model, OutputStream out) {
        bitOut = new BitOutput(out);
        this.model = model;
    }
    
    public void encodeSymbol(int symbol) throws IOException {
        model.encoding[symbol].write(bitOut);
    }
    
    public void close() throws IOException {
        model.encoding[model.encoding.length - 1].write(bitOut);//EOF
        // regrettable we can't flush without closing.  Yes bitoutput has flush
        // but it doesn't write the last fractional bit.  Could add that if we needed.
        bitOut.close();
    }
}

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
import java.io.OutputStream;

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

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

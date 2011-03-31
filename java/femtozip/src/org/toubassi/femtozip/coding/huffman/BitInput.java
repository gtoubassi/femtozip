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

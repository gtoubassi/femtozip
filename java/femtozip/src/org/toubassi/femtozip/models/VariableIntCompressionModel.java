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
package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

public class VariableIntCompressionModel extends CompressionModel {

    public void load(DataInputStream in) throws IOException {
    }

    public void save(DataOutputStream out) throws IOException {
    }
    
    public void build(DocumentList documents) {
    }
    
    
    public void encodeLiteral(int aByte) {
        throw new UnsupportedOperationException();
    }

    public void encodeSubstring(int offset, int length) {
        throw new UnsupportedOperationException();
    }

    public void endEncoding() {
        throw new UnsupportedOperationException();
    }
    
    public void compress(byte[] data, OutputStream out) throws IOException {
        if (data.length == 0) {
            return;
        }
        // If its too big to hold an int, or it has a leading 0, we can't do it (leading zeros will get lost in the encoding).
        if (data.length > 10 || data[0] == '0') {
            compressAsNonInt(data, out);
            return;
        }
        
        for (int i = 0, count = data.length; i < count; i++) {
            if (data[i] < '0' || data[i] > '9') {
                compressAsNonInt(data, out);
                return;
            }
        }
        
        long l = Long.parseLong(new String(data, "UTF-8"));
        int i = (int)l;
        if (i != l) {
            compressAsNonInt(data, out);
            return;
        }
        while ((i & ~0x7F) != 0) {
            out.write((byte)((i & 0x7f) | 0x80));
            i >>>= 7;
       }
        out.write(i);
    }
    
    private static byte[] padding = new byte[6];
    
    private void compressAsNonInt(byte[] data, OutputStream out) throws IOException {
        out.write(padding);
        out.write(data);
    }
    
    private byte[] decompressAsNonInt(byte[] compressedData) {
        return Arrays.copyOfRange(compressedData, 6, compressedData.length);
    }
    
    public byte[] decompress(byte[] compressedData) {
        if (compressedData.length == 0) {
            return compressedData;
        }
        if (compressedData.length > 5) {
            return decompressAsNonInt(compressedData);
        }
        
        int index = 0;
        byte b = compressedData[index++];
        int i = b & 0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
          b = compressedData[index++];
          i |= (b & 0x7F) << shift;
        }
        
        String s = Integer.toString(i);
        try {
            return s.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}

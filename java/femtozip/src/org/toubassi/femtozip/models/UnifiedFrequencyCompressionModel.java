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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeReader;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeWriter;
import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class UnifiedFrequencyCompressionModel extends CompressionModel implements SubstringPacker.Consumer {
    private static final int SUBSTRING_SYMBOL = 256;

    private FrequencyCodeModel codeModel;

    public void load(DataInputStream in) throws IOException {
        super.load(in);
        codeModel = new FrequencyCodeModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        super.save(out);
        codeModel.save(out);
    }
    
    public void build(DocumentList documents) throws IOException {
        buildDictionaryIfUnspecified(documents);
        codeModel = ((ModelBuilder)buildEncodingModel(documents)).createModel();
    }
    
    public SubstringPacker.Consumer createModelBuilder() {
        return new ModelBuilder();
    }
    
    public void compress(byte[] data, OutputStream out) throws IOException {
        getSubstringPacker().pack(data, this, new ArithCodeWriter(out, codeModel));
    }
    
    public void encodeLiteral(int aByte, Object context) {
        try {
            ArithCodeWriter writer = (ArithCodeWriter)context;
            writer.writeSymbol(aByte);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encodeSubstring(int offset, int length, Object context) {
        try {
            ArithCodeWriter writer = (ArithCodeWriter)context;
            writer.writeSymbol(SUBSTRING_SYMBOL);
        
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            writer.writeSymbol(length);
            
            offset = -offset;
            if (offset < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Offset " + offset + " out of range [1, 65535]");
            }
            writer.writeSymbol(offset & 0xff);
            writer.writeSymbol((offset >> 8) & 0xff);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void endEncoding(Object context) {
        try {
            ArithCodeWriter writer = (ArithCodeWriter)context;
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public byte[] decompress(byte[] compressedBytes) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
            ArithCodeReader reader = new ArithCodeReader(bytesIn, codeModel);
            SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
        
            int nextSymbol;
            while ((nextSymbol = reader.readSymbol()) != -1) {
                if (nextSymbol == SUBSTRING_SYMBOL) {
                    int length = reader.readSymbol();
                    int offset = reader.readSymbol() | (reader.readSymbol() << 8);
                    offset = -offset;
                    unpacker.encodeSubstring(offset, length, null);
                }
                else {
                    unpacker.encodeLiteral(nextSymbol, null);
                }
            }
            unpacker.endEncoding(null);
            return unpacker.getUnpackedBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ModelBuilder implements SubstringPacker.Consumer {
        int[] histogram = new int[256 + 1 + 1]; // 256 for each unique byte, 1 for marking the start of a substring reference, and 1 for EOF.
        
        public void encodeLiteral(int aByte, Object context) {
            histogram[aByte]++;
        }

        public void endEncoding(Object context) {
            histogram[histogram.length - 1]++;
        }

        public void encodeSubstring(int offset, int length, Object context) {
            histogram[SUBSTRING_SYMBOL]++;
            
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            histogram[length]++;
            
            offset = -offset;
            if (length < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Length " + length + " out of range [1, 65535]");
            }
            histogram[offset & 0xff]++;
            histogram[(offset >> 8) & 0xff]++;
        }

        public FrequencyCodeModel createModel() {
            return new FrequencyCodeModel(histogram, false);
        }
    }
}

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

public class SplitFrequencyCompressionModel extends CompressionModel {
    private static final int SUBSTRING_SYMBOL = 256;
    
    private SplitFrequencyCodeModel codeModel;
    private ArithCodeWriter writer;
    

    public void load(DataInputStream in) throws IOException {
        super.load(in);
        codeModel = new SplitFrequencyCodeModel(in);
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
        writer = new ArithCodeWriter(out, codeModel);
        super.compress(data, out);
        writer.close();
        writer = null;
    }
    
    public void encodeLiteral(int aByte) {
        try {
            writer.writeSymbol(aByte);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encodeSubstring(int offset, int length) {
        try {
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

    public void endEncoding() {
    }
    
    public byte[] decompress(byte[] compressedBytes) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
            ArithCodeReader reader = new ArithCodeReader(bytesIn, codeModel);
            SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
        
            int nextSymbol;
            while ((nextSymbol = reader.readSymbol()) != -1) {
                if (nextSymbol == SplitFrequencyCodeModel.SUBSTRING_SYMBOL) {
                    int length = reader.readSymbol();
                    int offset = reader.readSymbol() | (reader.readSymbol() << 8);
                    offset = -offset;
                    unpacker.encodeSubstring(offset, length);
                }
                else {
                    unpacker.encodeLiteral(nextSymbol);
                }
            }
            unpacker.endEncoding();
            return unpacker.getUnpackedBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class ModelBuilder implements SubstringPacker.Consumer {
        private int[] literalHistogram = new int[256 + 1 + 1]; // 256 for each unique byte, 1 for marking the start of a substring reference, and 1 for EOF.
        private int[] substringHistogram = new int[256]; // 256 for each unique byte
        
        public void encodeLiteral(int aByte) {
            literalHistogram[aByte]++;
        }

        public void endEncoding() {
            literalHistogram[literalHistogram.length - 1]++;
        }

        public void encodeSubstring(int offset, int length) {
            literalHistogram[SplitFrequencyCodeModel.SUBSTRING_SYMBOL]++;
            
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            substringHistogram[length]++;
            
            offset = -offset;
            if (length < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Length " + length + " out of range [1, 65535]");
            }
            substringHistogram[offset & 0xff]++;
            substringHistogram[(offset >> 8) & 0xff]++;
        }

        public SplitFrequencyCodeModel createModel() {
            return new SplitFrequencyCodeModel(new FrequencyCodeModel(literalHistogram, false),
                    new FrequencyCodeModel(substringHistogram, false, false));
        }
    }
    
}

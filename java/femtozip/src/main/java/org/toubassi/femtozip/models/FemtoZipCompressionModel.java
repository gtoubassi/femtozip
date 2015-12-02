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
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
import org.toubassi.femtozip.coding.huffman.HuffmanDecoder;
import org.toubassi.femtozip.coding.huffman.HuffmanEncoder;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class FemtoZipCompressionModel extends CompressionModel {
    
    private FemtoZipHuffmanModel codeModel;
    
    public void load(DataInputStream in) throws IOException {
        super.load(in);
        codeModel = new FemtoZipHuffmanModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        super.save(out);
        codeModel.save(out);
    }

    public void build(DocumentList documents) throws IOException {
        buildDictionaryIfUnspecified(documents);
        codeModel = ((ModelBuilder)buildEncodingModel(documents)).createModel();
    }
    
    protected SubstringPacker.Consumer createModelBuilder() {
        return new ModelBuilder();
    }
    
    public void compress(byte[] data, OutputStream out) throws IOException {
        getSubstringPacker().pack(data, this, new HuffmanEncoder(codeModel.createModel(), out));
    }
    
    public void encodeLiteral(int aByte, Object context) {
        try {
            HuffmanEncoder encoder = (HuffmanEncoder)context;
            encoder.encodeSymbol(aByte);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encodeSubstring(int offset, int length, Object context) {
        try {
            HuffmanEncoder encoder = (HuffmanEncoder)context;
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            encoder.encodeSymbol(256 + length);
            
            offset = -offset;
            if (offset < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Offset " + offset + " out of range [1, 65535]");
            }
            encoder.encodeSymbol(offset & 0xf);
            encoder.encodeSymbol((offset >> 4) & 0xf);
            encoder.encodeSymbol((offset >> 8) & 0xf);
            encoder.encodeSymbol((offset >> 12) & 0xf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void endEncoding(Object context) {
        try {
            HuffmanEncoder encoder = (HuffmanEncoder)context;
            encoder.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public byte[] decompress(byte[] compressedBytes) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
            HuffmanDecoder decoder = new HuffmanDecoder(codeModel.createModel(), bytesIn);
            SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
        
            int nextSymbol;
            while ((nextSymbol = decoder.decodeSymbol()) != -1) {
                if (nextSymbol > 255) {
                    int length = nextSymbol - 256;
                    int offset = decoder.decodeSymbol() | (decoder.decodeSymbol() << 4) | (decoder.decodeSymbol() << 8) | (decoder.decodeSymbol() << 12);
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
    
    
    private class ModelBuilder implements SubstringPacker.Consumer {
        private int[] literalLengthHistogram = new int[256 + 256 + 1]; // 256 for each unique literal byte, 256 for all possible length, plus 1 for EOF
        private int[] offsetHistogramNibble0 = new int[16];
        private int[] offsetHistogramNibble1 = new int[16];
        private int[] offsetHistogramNibble2 = new int[16];
        private int[] offsetHistogramNibble3 = new int[16];
        
        public void encodeLiteral(int aByte, Object context) {
            literalLengthHistogram[aByte]++;
        }
        
        public void endEncoding(Object context) {
            literalLengthHistogram[literalLengthHistogram.length - 1]++;
        }

        public void encodeSubstring(int offset, int length, Object context) {
            
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            literalLengthHistogram[256 + length]++;
            
            offset = -offset;
            if (length < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Length " + length + " out of range [1, 65535]");
            }
            offsetHistogramNibble0[offset & 0xf]++;
            offsetHistogramNibble1[(offset >> 4) & 0xf]++;
            offsetHistogramNibble2[(offset >> 8) & 0xf]++;
            offsetHistogramNibble3[(offset >> 12) & 0xf]++;
        }

        public FemtoZipHuffmanModel createModel() {
            return new FemtoZipHuffmanModel(
                    new FrequencyHuffmanModel(literalLengthHistogram, false),
                    new FrequencyHuffmanModel(offsetHistogramNibble0, false),
                    new FrequencyHuffmanModel(offsetHistogramNibble1, false),
                    new FrequencyHuffmanModel(offsetHistogramNibble2, false),
                    new FrequencyHuffmanModel(offsetHistogramNibble3, false));
        }
    }
}

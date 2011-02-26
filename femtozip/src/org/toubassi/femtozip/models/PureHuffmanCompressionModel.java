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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.HuffmanDecoder;
import org.toubassi.femtozip.coding.huffman.HuffmanEncoder;
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;

public class PureHuffmanCompressionModel extends CompressionModel {

    private FrequencyHuffmanModel codeModel;
    
    public void load(DataInputStream in) throws IOException {
        codeModel = new FrequencyHuffmanModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        codeModel.save(out);
    }
    
    public void build(DocumentList documents) {
        try {
            int[] histogram = new int[256 + 1]; // +1 for EOF
            
            for (int i = 0, count = documents.size(); i < count; i++) {
                byte[] bytes = documents.get(i);
                for (int j = 0, jcount = bytes.length; j < jcount; j++) {
                    histogram[((int)bytes[j]) & 0xff]++;
                }
                histogram[histogram.length - 1]++;
            }

            codeModel = new FrequencyHuffmanModel(histogram, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        HuffmanEncoder encoder = new HuffmanEncoder(codeModel, out);
        for (int i = 0, count = data.length; i < count; i++) {
            encoder.encodeSymbol(((int)data[i]) & 0xff);
        }
        encoder.close();
        encoder = null;
    }
    
    public byte[] decompress(byte[] compressedData) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedData);
            HuffmanDecoder decoder = new HuffmanDecoder(codeModel, bytesIn);
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(compressedData.length * 2);
            
            int nextSymbol;
            while ((nextSymbol = decoder.decodeSymbol()) != -1) {
                bytesOut.write((byte)nextSymbol);
            }
            return bytesOut.toByteArray();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

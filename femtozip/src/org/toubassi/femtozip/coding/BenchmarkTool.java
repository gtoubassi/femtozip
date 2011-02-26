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
package org.toubassi.femtozip.coding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;

import org.toubassi.femtozip.coding.arithmetic.ArithCodeReader;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeWriter;
import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
import org.toubassi.femtozip.coding.huffman.HuffmanDecoder;
import org.toubassi.femtozip.coding.huffman.HuffmanEncoder;
import org.toubassi.femtozip.util.StreamUtil;

public class BenchmarkTool {

    public static void testHuffman(int[] histogram, byte[] input, boolean allSymbolsSampled) throws IOException {
        long start = System.nanoTime();
        FrequencyHuffmanModel huffmanModel = new FrequencyHuffmanModel(histogram, allSymbolsSampled);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder(huffmanModel, bytesOut);
        for (int i = 0, count = input.length; i < count; i++) {
            huffmanEncoder.encodeSymbol(((int)input[i]) & 0xff);
        }
        
        huffmanEncoder.close();
        
        long duration = System.nanoTime() - start;

        byte[] compressedBytes = bytesOut.toByteArray();
        
        DecimalFormat format = new DecimalFormat("#.##");
        System.out.println("Huffman all symbols sampled = " + allSymbolsSampled + ": " + format.format(100f*compressedBytes.length/input.length) + " (" + bytesOut.size() + " bytes) in " + format.format(duration/1000000.0) + "ms");
        
        
        bytesOut.reset();
        HuffmanDecoder huffmanDecoder = new HuffmanDecoder(huffmanModel, new ByteArrayInputStream(compressedBytes));
        int symbol;
        while ((symbol = huffmanDecoder.decodeSymbol()) != -1) {
            bytesOut.write(symbol);
        }
        
        if (!Arrays.equals(input, bytesOut.toByteArray())) {
            System.out.println("Error: decoded bytes do not match!!!");
        }
    }
    
    public static void testArithmetic(int[] histogram, byte[] input, boolean allSymbolsSampled) throws IOException {
        long start = System.nanoTime();
        FrequencyCodeModel model = new FrequencyCodeModel(histogram, allSymbolsSampled);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ArithCodeWriter writer = new ArithCodeWriter(bytesOut, model);
        for (int i = 0, count = input.length; i < count; i++) {
            writer.writeSymbol(((int)input[i]) & 0xff);
        }
        
        writer.close();

        long duration = System.nanoTime() - start;

        byte[] compressedBytes = bytesOut.toByteArray();
        
        DecimalFormat format = new DecimalFormat("#.##");
        System.out.println("Arithmetic all symbols sampled = " + allSymbolsSampled + ": " + format.format(100f*compressedBytes.length/input.length) + " (" + bytesOut.size() + " bytes) in " + format.format(duration/1000000.0) + "ms");

        bytesOut.reset();
        ArithCodeReader reader = new ArithCodeReader(new ByteArrayInputStream(compressedBytes), model);
        int symbol;
        while ((symbol = reader.readSymbol()) != -1) {
            bytesOut.write(symbol);
        }
        
        if (!Arrays.equals(input, bytesOut.toByteArray())) {
            System.out.println("Error: decoded bytes do not match!!!");
        }
    }
    
    public static void main(String[] args) throws IOException {
        byte[] input = StreamUtil.readAll(System.in);
        
        System.out.println("Total input sized: " + input.length + " bytes");
        
        int histogram[] = FrequencyCodeModel.computeHistogramWithEOFSymbol(input);
        
        testHuffman(histogram, input, true);
        testHuffman(histogram, input, false);
        testArithmetic(histogram, input, true);
        testArithmetic(histogram, input, false);
        
        
    }
}

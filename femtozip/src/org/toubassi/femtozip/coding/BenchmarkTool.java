package org.toubassi.femtozip.coding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import org.toubassi.femtozip.coding.arithmetic.ArithCodeWriter;
import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;
import org.toubassi.femtozip.coding.huffman.HuffmanEncoder;
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
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
        
        DecimalFormat format = new DecimalFormat("#.##");
        System.out.println("Huffman all symbols sampled = " + allSymbolsSampled + ": " + format.format(100f*bytesOut.size()/input.length) + " (" + bytesOut.size() + " bytes) in " + format.format(duration/1000000.0) + "ms");
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
        
        DecimalFormat format = new DecimalFormat("#.##");
        System.out.println("Arithmetic all symbols sampled = " + allSymbolsSampled + ": " + format.format(100f*bytesOut.size()/input.length) + " (" + bytesOut.size() + " bytes) in " + format.format(duration/1000000.0) + "ms");
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

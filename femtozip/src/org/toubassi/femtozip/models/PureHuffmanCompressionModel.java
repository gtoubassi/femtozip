package org.toubassi.femtozip.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.AbstractCompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.HuffmanDecoder;
import org.toubassi.femtozip.coding.huffman.HuffmanEncoder;
import org.toubassi.femtozip.coding.huffman.HuffmanModel;

public class PureHuffmanCompressionModel extends AbstractCompressionModel {

    private HuffmanModel codeModel;
    
    public void load(DataInputStream in) throws IOException {
        codeModel = new HuffmanModel(in);
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
            }

            codeModel = new HuffmanModel(histogram, false);
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

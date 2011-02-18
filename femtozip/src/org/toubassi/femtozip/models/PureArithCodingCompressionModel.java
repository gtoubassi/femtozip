package org.toubassi.femtozip.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeReader;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeWriter;
import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;

public class PureArithCodingCompressionModel extends CompressionModel {
    private FrequencyCodeModel codeModel;

    public void load(DataInputStream in) throws IOException {
        codeModel = new FrequencyCodeModel(in);
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
            
            codeModel = new FrequencyCodeModel(histogram, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encodeLiteral(int aByte) {
    }

    public void encodeSubstring(int offset, int length) {
    }

    public void compress(byte[] data, OutputStream out) throws IOException {
        ArithCodeWriter writer = new ArithCodeWriter(out, codeModel);
        for (int i = 0, count = data.length; i < count; i++) {
            writer.writeSymbol(((int)data[i]) & 0xff);
        }
        writer.close();
        writer = null;
    }
    
    public byte[] decompress(byte[] compressedData) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedData);
            ArithCodeReader reader = new ArithCodeReader(bytesIn, codeModel);
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(compressedData.length * 2);
            
            int nextSymbol;
            while ((nextSymbol = reader.readSymbol()) != -1) {
                bytesOut.write((byte)nextSymbol);
            }
            return bytesOut.toByteArray();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

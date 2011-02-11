package org.toubassi.femtozip.encoding.purehuffman;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.encoding.benchmark.BenchmarkEncodingModel;
import org.toubassi.femtozip.huffman.Decoder;
import org.toubassi.femtozip.huffman.Encoder;
import org.toubassi.femtozip.huffman.HuffmanModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class PureHuffmanEncodingModel implements EncodingModel {
    
    private HuffmanModel codeModel;

    private ModelBuilder modelBuilder; // only used during model building
    private OutputStream out;
    private SubstringUnpacker unpacker;
    private BenchmarkEncodingModel benchmarkModel;
    
    public PureHuffmanEncodingModel(BenchmarkEncodingModel model) {
        this.benchmarkModel = model;
    }
    
    private byte[] getDictionary() {
        return benchmarkModel.getDictionary();
    }
    
    public void load(DataInputStream in) throws IOException {
        codeModel = new HuffmanModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        codeModel.save(out);
    }

    public void beginModelConstruction(byte[] dictionary) {
        modelBuilder = new ModelBuilder();
    }

    public void addDocumentToModel(byte[] document) {
        for (int i = 0, count = document.length; i < count; i++) {
            modelBuilder.encodeLiteral(((int)document[i]) & 0xff);
        }
    }

    public void endModelConstruction() {
        codeModel = modelBuilder.createModel();
        modelBuilder = null;
    }

    public void beginEncoding(OutputStream out) {
        this.out = out;
        unpacker = new SubstringUnpacker(getDictionary());
    }

    public void encodeLiteral(int aByte) {
        unpacker.encodeLiteral(aByte);
    }

    public void encodeSubstring(int offset, int length) {
        unpacker.encodeSubstring(offset, length);
    }

    public void endEncoding() {
        try {
            unpacker.endEncoding();
            byte[] rawBytes = unpacker.getUnpackedBytes();
            Encoder encoder = new Encoder(codeModel, out);
            for (int i = 0, count = rawBytes.length; i < count; i++) {
                encoder.encodeSymbol(((int)rawBytes[i]) & 0xff);
            }
            encoder.close();
            out = null;
            unpacker = null;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void decode(byte[] compressedBytes, SubstringPacker.Consumer consumer) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
            Decoder decoder = new Decoder(codeModel, bytesIn);
            
            int nextSymbol;
            while ((nextSymbol = decoder.decodeSymbol()) != -1) {
                consumer.encodeLiteral(nextSymbol);
            }
            consumer.endEncoding();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    private class ModelBuilder implements SubstringPacker.Consumer {
        private int[] literalHistogram = new int[256 + 1]; // 256 for each unique literal byte, plus 1 for EOF
        
        public void encodeLiteral(int aByte) {
            literalHistogram[aByte]++;
        }

        public void encodeSubstring(int offset, int length) {
            throw new UnsupportedOperationException();
        }

        public void endEncoding() {
        }
        
        public HuffmanModel createModel() {
            return new HuffmanModel(literalHistogram, false);
        }
    }
}

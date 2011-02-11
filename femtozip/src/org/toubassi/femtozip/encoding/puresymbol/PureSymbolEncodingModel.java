package org.toubassi.femtozip.encoding.puresymbol;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.encoding.arithcoding.ArithCodeReader;
import org.toubassi.femtozip.encoding.arithcoding.ArithCodeWriter;
import org.toubassi.femtozip.encoding.arithcoding.FrequencyCodeModel;
import org.toubassi.femtozip.encoding.benchmark.BenchmarkEncodingModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class PureSymbolEncodingModel implements EncodingModel {
    
    private FrequencyCodeModel codeModel;

    private ModelBuilder modelBuilder; // only used during model building
    private OutputStream out;
    private SubstringUnpacker unpacker;
    private BenchmarkEncodingModel benchmarkModel;
    
    public PureSymbolEncodingModel(BenchmarkEncodingModel model) {
        this.benchmarkModel = model;
    }
    
    private byte[] getDictionary() {
        return benchmarkModel.getDictionary();
    }
    
    public void load(DataInputStream in) throws IOException {
        codeModel = new FrequencyCodeModel(in);
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
            ArithCodeWriter writer = new ArithCodeWriter(out, codeModel);
            for (int i = 0, count = rawBytes.length; i < count; i++) {
                writer.writeSymbol(((int)rawBytes[i]) & 0xff);
            }
            writer.flush();
            writer.close();
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
            ArithCodeReader reader = new ArithCodeReader(bytesIn, codeModel);
            
            int nextSymbol;
            while ((nextSymbol = reader.readSymbol()) != -1) {
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
        
        public FrequencyCodeModel createModel() {
            return new FrequencyCodeModel(literalHistogram, false);
        }
    }
}

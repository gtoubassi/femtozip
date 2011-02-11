package org.toubassi.femtozip.encoding.splitfrequency;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.encoding.arithcoding.ArithCodeReader;
import org.toubassi.femtozip.encoding.arithcoding.ArithCodeWriter;
import org.toubassi.femtozip.encoding.arithcoding.FrequencyCodeModel;
import org.toubassi.femtozip.substring.SubstringPacker;

public class SplitFrequencyEncodingModel implements EncodingModel {
    private SplitFrequencyCodeModel codeModel;
    
    private ModelBuilder modelBuilder; // only used during model building
    private SubstringPacker modelBuildingPacker; // only used during model building
    private ArithCodeWriter writer; // only used during symbol encoding
    
    public void load(DataInputStream in) throws IOException {
        codeModel = new SplitFrequencyCodeModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        codeModel.save(out);
    }

    public void beginModelConstruction(byte[] dictionary) {
        modelBuilder = new ModelBuilder();
        modelBuildingPacker = new SubstringPacker(dictionary);
    }

    public void addDocumentToModel(byte[] document) {
        modelBuildingPacker.pack(document, modelBuilder);
    }

    public void endModelConstruction() {
        codeModel = modelBuilder.createModel();
        modelBuilder = null;
    }


    public void beginEncoding(OutputStream out) {
        writer = new ArithCodeWriter(out, codeModel);
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
            writer.writeSymbol(SplitFrequencyCodeModel.SUBSTRING_SYMBOL);
        
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
        try {
            writer.flush();
            writer.close();
            writer = null;
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
                if (nextSymbol == SplitFrequencyCodeModel.SUBSTRING_SYMBOL) {
                    int length = reader.readSymbol();
                    int offset = reader.readSymbol() | (reader.readSymbol() << 8);
                    offset = -offset;
                    consumer.encodeSubstring(offset, length);
                }
                else {
                    consumer.encodeLiteral(nextSymbol);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        consumer.endEncoding();
    }

    private class ModelBuilder implements SubstringPacker.Consumer {
        private int[] literalHistogram = new int[256 + 1 + 1]; // 256 for each unique byte, 1 for marking the start of a substring reference, and 1 for EOF.
        private int[] substringHistogram = new int[256]; // 256 for each unique byte
        
        public void encodeLiteral(int aByte) {
            literalHistogram[aByte]++;
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

        public void endEncoding() {
        }
        
        public SplitFrequencyCodeModel createModel() {
            return new SplitFrequencyCodeModel(new FrequencyCodeModel(literalHistogram, false),
                    new FrequencyCodeModel(substringHistogram, false, false));
        }
    }
    
}

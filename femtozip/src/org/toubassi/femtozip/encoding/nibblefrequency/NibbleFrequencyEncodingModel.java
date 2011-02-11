package org.toubassi.femtozip.encoding.nibblefrequency;

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

public class NibbleFrequencyEncodingModel implements EncodingModel {
    private NibbleFrequencyCodeModel codeModel;
    
    private ModelBuilder modelBuilder; // only used during model building
    private SubstringPacker modelBuildingPacker; // only used during model building
    private ArithCodeWriter writer; // only used during symbol encoding
    
    public void load(DataInputStream in) throws IOException {
        codeModel = new NibbleFrequencyCodeModel(in);
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
            writer.writeSymbol(NibbleFrequencyCodeModel.SUBSTRING_SYMBOL);
        
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            writer.writeSymbol(length & 0xf);
            writer.writeSymbol((length >> 4) & 0xf);
            
            offset = -offset;
            if (offset < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Offset " + offset + " out of range [1, 65535]");
            }
            writer.writeSymbol(offset & 0xf);
            writer.writeSymbol((offset >> 4) & 0xf);
            writer.writeSymbol((offset >> 8) & 0xf);
            writer.writeSymbol((offset >> 12) & 0xf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void endEncoding() {
        try {
            writer.flush();
            writer.close();
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
                if (nextSymbol == NibbleFrequencyCodeModel.SUBSTRING_SYMBOL) {
                    int length = reader.readSymbol() | (reader.readSymbol() << 4);
                    int offset = reader.readSymbol() | (reader.readSymbol() << 4) | (reader.readSymbol() << 8) | (reader.readSymbol() << 12);
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

    public class ModelBuilder implements SubstringPacker.Consumer {
        private int[] literalHistogram = new int[256 + 1 + 1]; // 256 for each unique byte, 1 for marking the start of a substring reference, and 1 for EOF.
        private int[] lengthHistogramNibble0 = new int[16];
        private int[] lengthHistogramNibble1 = new int[16];
        private int[] offsetHistogramNibble0 = new int[16];
        private int[] offsetHistogramNibble1 = new int[16];
        private int[] offsetHistogramNibble2 = new int[16];
        private int[] offsetHistogramNibble3 = new int[16];
        
        public void encodeLiteral(int aByte) {
            literalHistogram[aByte]++;
        }

        public void encodeSubstring(int offset, int length) {
            literalHistogram[NibbleFrequencyCodeModel.SUBSTRING_SYMBOL]++;
            
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            lengthHistogramNibble0[length & 0xf]++;
            lengthHistogramNibble1[(length >> 4) & 0xf]++;
            
            offset = -offset;
            if (length < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Length " + length + " out of range [1, 65535]");
            }
            offsetHistogramNibble0[offset & 0xf]++;
            offsetHistogramNibble1[(offset >> 4) & 0xf]++;
            offsetHistogramNibble2[(offset >> 8) & 0xf]++;
            offsetHistogramNibble3[(offset >> 12) & 0xf]++;
        }

        public void endEncoding() {
        }
        
        public NibbleFrequencyCodeModel createModel() {
            return new NibbleFrequencyCodeModel(
                    new FrequencyCodeModel(literalHistogram, false),
                    new FrequencyCodeModel(lengthHistogramNibble0, false, false),
                    new FrequencyCodeModel(lengthHistogramNibble1, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble0, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble1, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble2, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble3, false, false));
        }
    }
}

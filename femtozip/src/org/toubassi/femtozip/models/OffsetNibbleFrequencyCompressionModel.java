package org.toubassi.femtozip.models;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeReader;
import org.toubassi.femtozip.coding.arithmetic.ArithCodeWriter;
import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class OffsetNibbleFrequencyCompressionModel extends CompressionModel {
    
    private OffsetNibbleFrequencyCodeModel codeModel;
    private ArithCodeWriter writer; // only used during symbol encoding
    
    public void load(DataInputStream in) throws IOException {
        super.load(in);
        codeModel = new OffsetNibbleFrequencyCodeModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        super.save(out);
        codeModel.save(out);
    }
    
    public void build(DocumentList documents) throws IOException {
        buildDictionaryIfUnspecified(documents);
        codeModel = ((ModelBuilder)buildEncodingModel(documents)).createModel();
    }
    
    public SubstringPacker.Consumer createModelBuilder() {
        return new ModelBuilder();
    }
    
    public void compress(byte[] data, OutputStream out) throws IOException {
        writer = new ArithCodeWriter(out, codeModel);
        super.compress(data, out);
        writer.close();
        writer = null;
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
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            writer.writeSymbol(256 + length);
            
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
    }
    
    public byte[] decompress(byte[] compressedBytes) {
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
            ArithCodeReader reader = new ArithCodeReader(bytesIn, codeModel);
            SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
        
            int nextSymbol;
            while ((nextSymbol = reader.readSymbol()) != -1) {
                if (nextSymbol > 255) {
                    int length = nextSymbol - 256;
                    int offset = reader.readSymbol() | (reader.readSymbol() << 4) | (reader.readSymbol() << 8) | (reader.readSymbol() << 12);
                    offset = -offset;
                    unpacker.encodeSubstring(offset, length);
                }
                else {
                    unpacker.encodeLiteral(nextSymbol);
                }
            }
            unpacker.endEncoding();
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
        
        public void encodeLiteral(int aByte) {
            literalLengthHistogram[aByte]++;
        }
        
        public void endEncoding() {
            literalLengthHistogram[literalLengthHistogram.length - 1]++;
        }

        public void encodeSubstring(int offset, int length) {
            
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

        public OffsetNibbleFrequencyCodeModel createModel() {
            return new OffsetNibbleFrequencyCodeModel(
                    new FrequencyCodeModel(literalLengthHistogram, false),
                    new FrequencyCodeModel(offsetHistogramNibble0, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble1, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble2, false, false),
                    new FrequencyCodeModel(offsetHistogramNibble3, false, false));
        }
    }
}

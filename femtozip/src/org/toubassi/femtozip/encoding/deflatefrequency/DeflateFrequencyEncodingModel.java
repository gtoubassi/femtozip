package org.toubassi.femtozip.encoding.deflatefrequency;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.encoding.arithcoding.ArithCodeReader;
import org.toubassi.femtozip.encoding.arithcoding.ArithCodeWriter;
import org.toubassi.femtozip.encoding.arithcoding.FrequencyCodeModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringPacker.Consumer;

public class DeflateFrequencyEncodingModel implements EncodingModel {

    // extra bits for each length code
    private static final int[] NumLengthExtraBits = {
        0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0
    };
    
    private static final byte[] LengthCodes = {
        0,  1,  2,  3,  4,  5,  6,  7,  8,  8,  9,  9, 10, 10, 11, 11, 12, 12, 12, 12,
        13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 16, 16, 16, 16,
        17, 17, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 19, 19, 19, 19,
        19, 19, 19, 19, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
        21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22,
        22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
        25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
        27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 28
    };
    
    private static final int[] LengthBase = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56,
        64, 80, 96, 112, 128, 160, 192, 224, 0
    };
    

    private static final int[] NumOffsetExtraBits = {
        0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,13,13,13,13
    };
    
    private static final byte[] OffsetCodes = {
        0,  1,  2,  3,  4,  4,  5,  5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  8,
        8,  8,  8,  8,  9,  9,  9,  9,  9,  9,  9,  9, 10, 10, 10, 10, 10, 10, 10, 10,
        10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
        11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
        12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13, 13,
        13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
        13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
        14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
        14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
        14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 15, 15, 15, 15, 15, 15, 15,
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,  0,  0, 16, 17,
        18, 18, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 22,
        23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27,
        27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
        27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
        28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
        28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
        28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
        29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
        29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
        29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
        
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        30, 30, 30, 30,

        31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
        31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
        31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
        31, 31, 31, 31,
        
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32,
        
        33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
        33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
        33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
        33, 33, 33, 33,
    };
    
    private static final int[] OffsetBase = {
        0,   1,      2,     3,     4,    6,     8,    12,    16,     24,
        32,  48,     64,    96,   128,  192,   256,   384,   512,    768,
        1024, 1536,  2048,  3072,  4096,  6144,  8192, 12288, 16384, 24576,
        8192*4, 8192*5, 8192*6, 8192*7
    };
    
    private DeflateFrequencyCodeModel codeModel;
    
    private ModelBuilder modelBuilder; // only used during model building
    private SubstringPacker modelBuildingPacker; // only used during model building
    private ArithCodeWriter writer; // only used during symbol encoding
    private ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    private OutputStream out;
    private BitSet extraBits = new BitSet();
    private int extraBitCount;

    
    private static int symbolCodeForOffset(int offset){
        return ((offset) < 256 ? OffsetCodes[offset] : OffsetCodes[256+((offset)>>>7)]);
    }

    
    public void load(DataInputStream in) throws IOException {
        codeModel = new DeflateFrequencyCodeModel(in);
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
        bytesOut.reset();
        extraBits.clear();
        extraBitCount = 0;
        this.writer = new ArithCodeWriter(bytesOut, codeModel);
        this.out = out;
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

            length -= 3;
            int lengthCode = LengthCodes[length];
            writer.writeSymbol(256 + lengthCode);
            writeExtraBits(length, NumLengthExtraBits[lengthCode]);
            
            offset = -offset;
            if (offset < 0 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Offset " + offset + " out of range [0, 32767]");
            }
            offset--;
            int offsetCode = symbolCodeForOffset(offset);
            writer.writeSymbol(offsetCode);
            writeExtraBits(offset, NumOffsetExtraBits[offsetCode]);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void writeExtraBits(int value, int numExtraBits) throws IOException {
        while (numExtraBits > 0) {
            extraBits.set(extraBitCount++, (value & 1) == 1 ? true : false);
            value >>= 1;
            numExtraBits--;
        }
    }
    
    public void endEncoding() {
        try {
            writer.flush();
            writer.close();
            writer = null;

            int numBytes = extraBitCount / 8;
            if (extraBitCount % 8 != 0) {
                numBytes++;
            }
            if (numBytes > (1<<15) - 1) {
                throw new RuntimeException("Too many extra bits for this half assed test implementation");
            }
            if (numBytes < 255) {
                out.write(numBytes);
            }
            else {
                out.write(255);
                out.write((numBytes >>> 8) & 0xff);
                out.write((numBytes >>> 0) & 0xff);
            }
            int nextByte = 0;
            int numBits = 0;
            int bytesWritten = 0;
            for (int i = 0; i < extraBitCount; i++) {
                nextByte = (nextByte << 1) | (extraBits.get(i) ? 1 : 0);
                numBits++;
                if (numBits == 8) {
                    out.write(nextByte);
                    bytesWritten++;
                    numBits = 0;
                    nextByte = 0;
                }
            }
            if (bytesWritten < numBytes) {
                while (numBits++ < 8) {
                    nextByte <<= 1;
                }
                out.write(nextByte);
            }
            out.write(bytesOut.toByteArray());
            out = null;
            bytesOut.reset();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void decode(byte[] compressedBytes, SubstringPacker.Consumer consumer) {
        BitSet decodingExtraBits = new BitSet();
        int[] extraBitIndex = new int[1];
        
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
            
            int numExtraBitBytes = bytesIn.read();
            if (numExtraBitBytes == 255) {
                numExtraBitBytes = (short)((bytesIn.read() << 8) + (bytesIn.read() << 0));
            }
            int extraBitCount = 0;
            while (numExtraBitBytes > 0) {
                int bits = bytesIn.read();
                for (int i = 0; i < 8; i++) {
                    decodingExtraBits.set(extraBitCount++, ((bits & 0x80) != 0) ? true : false);
                    bits <<= 1;
                }
                
                numExtraBitBytes--;
            }

            ArithCodeReader reader = new ArithCodeReader(bytesIn, codeModel);
            
            int nextSymbol;
            while ((nextSymbol = reader.readSymbol()) != -1) {
                if (nextSymbol > 255) {
                    int lengthCode = nextSymbol - 256;
                    int lengthBase = LengthBase[lengthCode];
                    int length = readExtraBits(decodingExtraBits, extraBitIndex, reader, lengthBase, NumLengthExtraBits[lengthCode]);
                    length += 3;

                    int offsetCode = reader.readSymbol();
                    int offsetBase = OffsetBase[offsetCode];
                    int offset = readExtraBits(decodingExtraBits, extraBitIndex, reader, offsetBase, NumOffsetExtraBits[offsetCode]);

                    offset++;
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
    
    private int readExtraBits(BitSet decodingExtraBits, int[] extraBitIndex, ArithCodeReader reader, int base, int numExtraBits) throws IOException {
        if (numExtraBits == 0) {
            return base;
        }
        int bits = 0;
        int mask = 1;
        while (numExtraBits--  > 0) {
            if (decodingExtraBits.get(extraBitIndex[0]++)) {
                bits |= mask;
            }
            mask <<= 1;
        }
        
        return base + bits;
    }

    private class ModelBuilder implements Consumer {
        
        private int[] literalLengthHistogram = new int[256 + LengthBase.length + 1]; // 256 for each unique literal byte, 29 for length codes, plus 1 for eof
        private int[] offsetHistogram = new int[OffsetBase.length];
        
        public void encodeLiteral(int aByte) {
            literalLengthHistogram[aByte]++;
        }
        
        public void encodeSubstring(int offset, int length) {
            
            if (length < 3 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [3,255]");
            }
            
            length -= 3;
            literalLengthHistogram[256 + LengthCodes[length]]++;
            
            offset = -offset;
            if (offset < 0 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Offset " + offset + " out of range [1, 32767]");
            }
            offset--;
            offsetHistogram[symbolCodeForOffset(offset)]++;
        }
        
        public void endEncoding() {
        }

        public DeflateFrequencyCodeModel createModel() {
            return new DeflateFrequencyCodeModel(
                    new FrequencyCodeModel(literalLengthHistogram, false),
                    new FrequencyCodeModel(offsetHistogram, false, false));
        }
    }
}

package org.toubassi.femtozip.encoding.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.encoding.purehuffman.PureHuffmanEncodingModel;
import org.toubassi.femtozip.encoding.puresymbol.PureSymbolEncodingModel;
import org.toubassi.femtozip.substring.SubstringPacker.Consumer;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class BenchmarkEncodingModel implements EncodingModel {
    private static class ModelInfo implements Comparable<ModelInfo>{
        public String name;
        public EncodingModel model;
        public ByteArrayOutputStream bytesOut;
        public int totalCompressedSize;

        public ModelInfo(String name, EncodingModel model) {
            this.model = model;
            if (name == null) {
                name = model.getClass().getSimpleName().replace("EncodingModel", "");
            }
            this.name = name;
        }

        public int compareTo(ModelInfo other) {
            return totalCompressedSize - other.totalCompressedSize;
        }
    }
    
    private ArrayList<ModelInfo> modelInfos = new ArrayList<ModelInfo>();
    private ArrayList<ModelInfo> sortedInfos;
    private OutputStream output;
    private boolean verify;
    private byte[] dictionary;
    private SubstringUnpacker unpacker;
    private int totalSize;
    private int documentCount;
    
    public BenchmarkEncodingModel() {
        this(null);
    }
    
    public BenchmarkEncodingModel(String[] encodings) {
        this.verify = true;
        
        if (encodings == null) {
            encodings = "UnifiedFrequency,SplitFrequency,OffsetNibbleFrequency,NibbleFrequency,TripleNibbleFrequency,DeflateFrequency,GZip,GZip+Dictionary".split(",");            
        }
        
        for (String encoding : encodings) {
            EncodingModel model;
            
            if (encoding.equals("GZip")) {
                model = new DeflateEncodingModel(this, false);
            }
            else if (encoding.equals("GZip+Dictionary")) {
                model = new DeflateEncodingModel(this, true);
            }
            else if (encoding.equals("PureSymbol")) {
                model = new PureSymbolEncodingModel(this);
            }
            else if (encoding.equals("PureHuffman")) {
                model = new PureHuffmanEncodingModel(this);
            }
            else {
                model = CompressionModel.instantiateEncodingModel(encoding);
            }
            
            modelInfos.add(new ModelInfo(encoding, model));
        }
    }
    
    public byte[] getDictionary() {
        return dictionary;
    }
    
    public void load(DataInputStream in) throws IOException {
        int dictLength = in.readInt();
        dictionary = new byte[dictLength];
        int totalRead = 0;
        int numRead;
        while ((numRead = in.read(dictionary, totalRead, dictLength - totalRead)) >= 0 && totalRead < dictLength) {
            totalRead += numRead;
        }
        
        for (ModelInfo info : modelInfos) {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            totalRead = 0;
            while ((numRead = in.read(bytes, totalRead, length - totalRead)) >= 0 && totalRead < length) {
                totalRead += numRead;
            }
            
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
            info.model.load(new DataInputStream(bytesIn));
        }        
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(dictionary.length);
        out.write(dictionary);
        
        for (ModelInfo info : modelInfos) {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(bytesOut);
            info.model.save(dataOut);
            dataOut.close();
            out.writeInt(bytesOut.size());
            out.write(bytesOut.toByteArray());
        }
    }

    public void beginModelConstruction(byte[] dictionary) {
        this.dictionary = dictionary;
        documentCount = 0;
        for (ModelInfo info : modelInfos) {
            info.model.beginModelConstruction(dictionary);
        }
    }

    public void addDocumentToModel(byte[] document) {
        documentCount++;
        for (ModelInfo info : modelInfos) {
            info.model.addDocumentToModel(document);
        }
    }

    public void endModelConstruction() {
        for (ModelInfo info : modelInfos) {
            info.model.endModelConstruction();
        }
    }

    public void beginEncoding(OutputStream out) {
        output = out;
        unpacker = new SubstringUnpacker(dictionary);
        for (ModelInfo info : modelInfos) {
            info.bytesOut = new ByteArrayOutputStream();
            info.model.beginEncoding(info.bytesOut);
        }
    }

    public void encodeLiteral(int aByte) {
        unpacker.encodeLiteral(aByte);
        for (ModelInfo info : modelInfos) {
            info.model.encodeLiteral(aByte);
        }
    }

    public void encodeSubstring(int offset, int length) {
        for (ModelInfo info : modelInfos) {
            info.model.encodeSubstring(offset, length);
        }
        unpacker.encodeSubstring(offset, length);
    }

    public void endEncoding() {
        for (ModelInfo info : modelInfos) {
            info.model.endEncoding();
        }
        ByteArrayOutputStream smallest = null;

        unpacker.endEncoding();
        byte[] originalBytes = unpacker.getUnpackedBytes();
        totalSize += originalBytes.length;
        unpacker = null;
        
        for (ModelInfo info : modelInfos) {
            ByteArrayOutputStream bytesOut = info.bytesOut;
            if (smallest == null || bytesOut.size() < smallest.size()) {
                smallest = bytesOut;
            }
            
            info.totalCompressedSize += bytesOut.size();
            
            if (verify) {
                byte[] compressedBytes = bytesOut.toByteArray();
                SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
                info.model.decode(compressedBytes, unpacker);
                byte[] decompressedBytes = unpacker.getUnpackedBytes();
                if (!Arrays.equals(originalBytes, decompressedBytes)) {
                    throw new RuntimeException("Compress/Decompress round trip failed for encoder " + info.model.getClass().getSimpleName());
                }
                
            }
            
            info.bytesOut = null;
        }
        try {
            output.write(smallest.toByteArray());
            output = null;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        sortedInfos = null; // invalidate the cache
    }

    public void decode(byte[] encodedBytes, Consumer consumer) {
        throw new UnsupportedOperationException("Cannot decode with " + this.getClass().getSimpleName());
    }
    
    protected ArrayList<ModelInfo> getSortedModelInfos() {
        if (sortedInfos == null) {
            sortedInfos = new ArrayList<ModelInfo>(modelInfos);
            Collections.sort(sortedInfos);
        }
        return sortedInfos;
    }
    
    public int getTotalSize() {
        return totalSize;
    }
    
    public int getBestPerformingTotalCompressedSize() {
        int best = getSortedModelInfos().get(0).totalCompressedSize;
        return best > totalSize ? totalSize : best;
    }
    
    public EncodingModel getEncodingModel(int i) {
        return modelInfos.get(i).model;
    }
    
    public String getBestPerformingEncoding() {
        int best = getSortedModelInfos().get(0).totalCompressedSize;
        return best > totalSize ? "No Compression" : getSortedModelInfos().get(0).name;
    }
    
    public void dump() {
        if (totalSize <= 0) {
            return;
        }
        ArrayList<ModelInfo> sorted = getSortedModelInfos();
        for (ModelInfo info : sorted) {
            System.out.println(info.name + " " + Math.round(100f * info.totalCompressedSize / totalSize) + "% (" + info.totalCompressedSize + " bytes)");
        }
    }

}

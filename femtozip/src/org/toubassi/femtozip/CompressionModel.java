package org.toubassi.femtozip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;


public class CompressionModel {

    private DictionaryOptimizer dictOptimizer;
    private int maxDictionaryLength; // For testing/benchmarking
    private byte[] dictionary;
    private EncodingModel encodingModel;
    private ByteArrayOutputStream contiguousDocs;
    private ArrayList<byte[]> allDocs;

    public static EncodingModel instantiateEncodingModel(String encoding) {
        if (encoding.indexOf('.') == -1) {
            encoding = "org.toubassi.femtozip.encoding." + encoding.toLowerCase() + "." + encoding + "EncodingModel";
        }

        EncodingModel encodingModel = null;

        try {
            Class<?> cls = Class.forName(encoding);
            encodingModel = (EncodingModel)cls.newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
         return encodingModel;
    }
        
    public CompressionModel(EncodingModel encodingModel) {
        this.encodingModel = encodingModel;
    }
    
    public EncodingModel getEncodingModel() {
        return encodingModel;
    }
    
    public void setMaxDictionaryLength(int length) {
        maxDictionaryLength = length;
    }
    
    public int getMaxDictionaryLength() {
        return maxDictionaryLength;
    }
    
    public void setDictionary(byte[] dictionary) {
        if (maxDictionaryLength > 0 && dictionary.length > maxDictionaryLength) {
            // We chop off the front as important strings are packed towards the end for shorter lengths/offsets
            dictionary = Arrays.copyOfRange(dictionary, dictionary.length - maxDictionaryLength, dictionary.length);
        }
        this.dictionary = dictionary;
    }
    
    public byte[] getDictionary() {
        return dictionary;
    }
   
    public void beginModelConstruction() {
        contiguousDocs = new ByteArrayOutputStream();
        allDocs = new ArrayList<byte[]>();
    }
    
    public void addDocumentToModel(byte[] document) {
        try {
            allDocs.add(document);
            contiguousDocs.write(document);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void endModelConstruction() {
        // Allow a manually specified dictionary
        if (dictionary == null) {
            dictOptimizer = new DictionaryOptimizer(contiguousDocs.toByteArray());
            dictionary = dictOptimizer.optimize(64*1024);
        }
        
        encodingModel.beginModelConstruction(dictionary);
        for (byte[] document : allDocs) {
            encodingModel.addDocumentToModel(document);
        }
        encodingModel.endModelConstruction();
        allDocs = null;
        contiguousDocs = null;
    }
    
    private void load(DataInputStream in) throws IOException {
        int dictionaryLength = in.readInt();
        dictionary = dictionaryLength == -1 ? null : new byte[dictionaryLength];
        
        int totalRead = 0;
        int numRead = 0;
        while ((numRead = in.read(dictionary, totalRead, dictionaryLength - totalRead)) > -1 && totalRead < dictionaryLength) {
            totalRead += numRead;
        }
        
        if (totalRead != dictionaryLength) {
            throw new IOException("Bad model in stream.  Could not read dictionary of length " + dictionaryLength);
        }

        // Truncate if necessary
        setDictionary(dictionary);
        
        encodingModel.load(in);
    }

    public void load(String path) throws IOException {
        FileInputStream fileIn = new FileInputStream(path);
        BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
        DataInputStream in = new DataInputStream(bufferedIn);
        
        load(in);
        
        in.close();
    }
    
    private void save(DataOutputStream out) throws IOException {
        out.writeInt(dictionary == null ? -1 : dictionary.length);
        out.write(dictionary);
        encodingModel.save(out);
    }

    public void save(String path) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(path);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
        DataOutputStream out = new DataOutputStream(bufferedOut);
        
        save(out);
        
        out.close();
    }

    /**
     * For diagnostics/debugging
     */
    public void dump(String path) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(path);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
        PrintStream out = new PrintStream(bufferedOut);
        
        out.println("SuffixArray + LCP");
        dictOptimizer.dump(out);
        out.println("\nRepeated Substrings");
        dictOptimizer.dumpSubstrings(out);
        
        out.close();
    }
    
    public byte[] compress(byte[] bytes) {
        SubstringPacker packer = new SubstringPacker(dictionary);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        encodingModel.beginEncoding(bytesOut);
        packer.pack(bytes, encodingModel);
        return bytesOut.toByteArray();
    }
    
    public byte[] decompress(byte[] compressedBytes) {
        SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
        encodingModel.decode(compressedBytes, unpacker);
        return unpacker.getUnpackedBytes();
    }
}

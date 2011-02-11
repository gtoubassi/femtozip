package org.toubassi.femtozip.encoding.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.substring.SubstringPacker.Consumer;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class DeflateEncodingModel implements EncodingModel {
    
    private OutputStream out;
    private BenchmarkEncodingModel benchmarkModel;
    private SubstringUnpacker unpacker;
    private boolean useDictionaryForDeflate;

    public DeflateEncodingModel(BenchmarkEncodingModel model, boolean useDictionaryForDeflate) {
        this.benchmarkModel = model;
        this.useDictionaryForDeflate = useDictionaryForDeflate;
    }
    
    private byte[] getDictionary() {
        return benchmarkModel.getDictionary();
    }
    
    public void load(DataInputStream in) throws IOException {
    }

    public void save(DataOutputStream out) throws IOException {
    }

    public void beginModelConstruction(byte[] dictionary) {
    }

    public void addDocumentToModel(byte[] document) {
    }

    public void endModelConstruction() {
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
            byte[] compressedBytes = compress(useDictionaryForDeflate ? getDictionary() : null, rawBytes, 0, rawBytes.length);
            out.write(compressedBytes);
            out = null;
            unpacker = null;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void decode(byte[] encodedBytes, Consumer consumer) {
        byte[] bytes = decompress(useDictionaryForDeflate ? getDictionary() : null, encodedBytes, 0, encodedBytes.length);
        for (int i = 0, count = bytes.length; i < count; i++) {
            consumer.encodeLiteral(((int)bytes[i]) & 0xff);
        }
        consumer.endEncoding();

    }
    
    private byte[] decompress(byte[] dictionary, byte[] data, int offset, int length) {
        try {
            Inflater decompresser = new Inflater();
            decompresser.setInput(data, offset, length);
            byte[] result = new byte[1024];
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(2 * length);
            while (!decompresser.finished()) {
                int resultLength = decompresser.inflate(result);
                if (resultLength == 0 && decompresser.needsDictionary()) {
                    decompresser.setDictionary(dictionary);
                }
                if (resultLength > 0) {
                    bytesOut.write(result, 0, resultLength);
                }
            }
            decompresser.end();
            return bytesOut.toByteArray();
        }
        catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] compress (byte[] dictionary, byte[] input, int offset, int length) {
        Deflater compressor = new Deflater();

        /*
         * Create an expandable byte array to hold the compressed data.
         * You cannot use an array that's the same size as the original because
         * there is no guarantee that the compressed data will be smaller than
         * the uncompressed data.
         */
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);

        try {
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            if (dictionary != null) {
                compressor.setDictionary(dictionary);
            }

            // Give the compressor the data to compress
            compressor.setInput(input);
            compressor.finish();

            // Compress the data
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                bos.write(buf, 0, count);
            }

        } finally {
            compressor.end();
        }

            // Get the compressed data
        return bos.toByteArray();
    }
}

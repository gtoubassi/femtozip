package org.toubassi.femtozip.models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

public class GZipDictionaryCompressionModel extends CompressionModel {

    public void encodeLiteral(int aByte) {
        throw new UnsupportedOperationException();
    }

    public void encodeSubstring(int offset, int length) {
        throw new UnsupportedOperationException();
    }

    public void endEncoding() {
        throw new UnsupportedOperationException();
    }
    
    public void build(DocumentList documents) {
    }

    public void compress(byte[] data, OutputStream out) throws IOException {
        compress(out, dictionary, data, 0, data.length); 
    }

    protected void compress(OutputStream out, byte[] dictionary, byte[] input, int offset, int length) throws IOException {
        Deflater compressor = new Deflater();

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
                out.write(buf, 0, count);
            }

        } finally {
            compressor.end();
        }
    }
    
    public byte[] decompress(byte[] compressedData) {
        try {
            Inflater decompresser = new Inflater();
            decompresser.setInput(compressedData, 0, compressedData.length);
            byte[] result = new byte[1024];
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(2 * compressedData.length);
            while (!decompresser.finished()) {
                int resultLength = decompresser.inflate(result);
                if (resultLength == 0 && decompresser.needsDictionary()) {
                    byte[] dict;
                    
                    if (dictionary.length > (1 << 15) - 1) {
                        dict = Arrays.copyOfRange(dictionary, dictionary.length - ((1 << 15) - 1), dictionary.length);
                    }
                    else {
                        dict = dictionary;
                    }
                    decompresser.setDictionary(dict);
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

}

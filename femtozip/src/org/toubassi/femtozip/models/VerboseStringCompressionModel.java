package org.toubassi.femtozip.models;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class VerboseStringCompressionModel extends CompressionModel {
    private PrintWriter writer;

    public void build(DocumentList documents) throws IOException {
        buildDictionaryIfUnspecified(documents);
    }

    public void compress(byte[] data, OutputStream out) throws IOException {
        writer = new PrintWriter(out);
        super.compress(data, out);
        writer.close();
        writer = null;
    }

    public byte[] decompress(byte[] compressedData) {
        try {
            SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);
            String source = new String(compressedData, "UTF-8");
            for (int i = 0, count = source.length(); i < count; i++) {
                char ch = source.charAt(i);
                if (ch == '<') {
                    int rightAngleIndex = source.indexOf('>', i);
                    String substring = source.substring(i + 1, rightAngleIndex);
                    String[] parts = substring.split(",");
                    int offset = Integer.parseInt(parts[0]);
                    int length = Integer.parseInt(parts[1]);
                    
                    unpacker.encodeSubstring(offset, length);
                    // Skip past this in the outer loop
                    i = rightAngleIndex;
                }
                else {
                    unpacker.encodeLiteral((int)ch);
                }
            }
            unpacker.endEncoding();
            return unpacker.getUnpackedBytes();
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void encodeLiteral(int aByte) {
        writer.print((char)aByte);
    }

    public void endEncoding() {
    }

    public void encodeSubstring(int offset, int length) {
        writer.print('<');
        writer.print(offset);
        writer.print(',');
        writer.print(length);
        writer.print('>');
    }
}

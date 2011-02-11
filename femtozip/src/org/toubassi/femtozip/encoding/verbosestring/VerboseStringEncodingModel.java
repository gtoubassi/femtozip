package org.toubassi.femtozip.encoding.verbosestring;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.substring.SubstringPacker;

public class VerboseStringEncodingModel implements EncodingModel {
    private PrintWriter writer;

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
        writer = new PrintWriter(out);
    }

    public void encodeLiteral(int aByte) {
        writer.print((char)aByte);
    }

    public void encodeSubstring(int offset, int length) {
        writer.print('<');
        writer.print(offset);
        writer.print(',');
        writer.print(length);
        writer.print('>');
    }
    
    public void endEncoding() {
        writer.flush();
        writer.close();
    }
    
    public void decode(byte[] compressedBytes, SubstringPacker.Consumer consumer) {
        try {
            String source = new String(compressedBytes, "UTF-8");
            for (int i = 0, count = source.length(); i < count; i++) {
                char ch = source.charAt(i);
                if (ch == '<') {
                    int rightAngleIndex = source.indexOf('>', i);
                    String substring = source.substring(i + 1, rightAngleIndex);
                    String[] parts = substring.split(",");
                    int offset = Integer.parseInt(parts[0]);
                    int length = Integer.parseInt(parts[1]);
                    
                    consumer.encodeSubstring(offset, length);
                    // Skip past this in the outer loop
                    i = rightAngleIndex;
                }
                else {
                    consumer.encodeLiteral((int)ch);
                }
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        
        consumer.endEncoding();
    }
}

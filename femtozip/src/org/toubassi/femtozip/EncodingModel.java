package org.toubassi.femtozip;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.substring.SubstringPacker;


public interface EncodingModel extends SubstringPacker.Consumer {
    
    public void load(DataInputStream in) throws IOException;
    public void save(DataOutputStream out) throws IOException;
    
    public void beginModelConstruction(byte[] dictionary);
    public void addDocumentToModel(byte[] document);
    public void endModelConstruction();

    public void beginEncoding(OutputStream out);
    public void encodeLiteral(int aByte);
    public void encodeSubstring(int offset, int length);
    public void endEncoding();
    
    public void decode(byte[] encodedBytes, SubstringPacker.Consumer consumer);
}

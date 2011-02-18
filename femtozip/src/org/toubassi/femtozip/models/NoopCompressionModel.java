package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.AbstractCompressionModel;
import org.toubassi.femtozip.DocumentList;

public class NoopCompressionModel extends AbstractCompressionModel {
    
    public void load(DataInputStream in) throws IOException {
        // Nothing to save.  We override so the base class doesn't save the dictionary
    }

    public void save(DataOutputStream out) throws IOException {
        // Nothing to save.  We override so the base class doesn't save the dictionary
    }
    
    public void encodeLiteral(int aByte) {
        throw new UnsupportedOperationException();
    }

    public void encodeSubstring(int offset, int length) {
        throw new UnsupportedOperationException();
    }

    public void build(DocumentList documents) {
    }

    public void compress(byte[] data, OutputStream out) throws IOException {
        out.write(data);
    }
    
    public byte[] decompress(byte[] compressedData) {
        return compressedData;
    }

}

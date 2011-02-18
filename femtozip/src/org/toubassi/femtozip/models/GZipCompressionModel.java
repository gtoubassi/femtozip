package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GZipCompressionModel extends GZipDictionaryCompressionModel {

    public void load(DataInputStream in) throws IOException {
        // Nothing to save.  We override so the base class doesn't save the dictionary
    }

    public void save(DataOutputStream out) throws IOException {
        // Nothing to save.  We override so the base class doesn't save the dictionary
    }

    public void compress(byte[] data, OutputStream out) throws IOException {
        compress(out, null, data, 0, data.length); 
    }
}

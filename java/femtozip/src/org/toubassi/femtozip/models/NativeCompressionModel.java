package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

public class NativeCompressionModel extends CompressionModel {
    
    private static boolean nativeLibraryLoaded;

    protected long nativeModel;
    
    public NativeCompressionModel() {
        if (!nativeLibraryLoaded) {
            System.loadLibrary("jnifemtozip");
            nativeLibraryLoaded = true;
        }
    }

    public void encodeLiteral(int aByte, Object context) {
        throw new UnsupportedOperationException();
    }

    public void encodeSubstring(int offset, int length, Object context) {
        throw new UnsupportedOperationException();
    }

    public void endEncoding(Object context) {
        throw new UnsupportedOperationException();
    }

    public void load(DataInputStream in) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void save(DataOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }    

    public void compress(byte[] data, OutputStream out) throws IOException {
        // XXX Performance.  Lots of allocations.  Lots of copying.  Use a thread local?  Change this api?
        byte[] buf = new byte[data.length * 2];
        int length = compress(data, buf);
        
        if (length < 0) {
            buf = new byte[length];
            length = compress(data, buf);
            if (length < 0) {
                throw new IllegalStateException();
            }
        }
        
        out.write(buf, 0, length);
    }

    public byte[] decompress(byte[] compressedData) {
        // XXX Performance.  Lots of allocations.  Lots of copying.  Use a thread local?  Change this api?
        byte[] buf = new byte[compressedData.length * 20];
        int length = decompress(compressedData, buf);
        
        if (length < 0) {
            buf = new byte[length];
            length = decompress(compressedData, buf);
            if (length < 0) {
                throw new IllegalStateException();
            }
        }
        if (buf.length != length) {
            byte[] newbuf = new byte[length];
            System.arraycopy(buf, 0, newbuf, 0, length);
            buf = newbuf;
        }
        return buf;
    }

    
    public native void load(String path) throws IOException;
    
    public native void save(String path) throws IOException;
    
    public native void build(DocumentList documents) throws IOException;

    public native int compress(byte[] data, byte[] output);
    
    public native int decompress(byte[] compressedData, byte[] decompressedData);

    @Override
    protected void finalize() {
        free();
    }

    protected native synchronized void free();
}

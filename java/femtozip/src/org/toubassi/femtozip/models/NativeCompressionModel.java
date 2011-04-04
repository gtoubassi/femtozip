/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

/**
 * NativeCompressionModel provides an interface to the native implementation
 * of FemtoZip, within the CompressionModel abstraction.  Some things to note.
 * To use this implementation, you must have built the native shared library.
 * See https://github.com/gtoubassi/femtozip/wiki/How-to-build.
 * 
 * The major difference between native and pure java implementations is that
 * with the native implementation, you call build, and load(String) directly
 * on an instance of this class, vs the buildOptimalModel and loadModel
 * statics on CompressionModel.
 * 
 * For a simple JNI example, see the org.toubassi.femtozip.models.NativeCompressionModelTest
 * JUnit test case in the source distribution of FemtoZip at
 * http://github.com/gtoubassi/femtozip
 */
public class NativeCompressionModel extends CompressionModel {
    
    private static boolean nativeLibraryLoaded;

    protected long nativeModel;
    
    public NativeCompressionModel() {
        if (!nativeLibraryLoaded) {
            System.loadLibrary("jnifzip");
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

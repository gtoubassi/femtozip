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
package org.toubassi.femtozip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.OptimizingCompressionModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.util.StreamUtil;

public abstract class CompressionModel implements SubstringPacker.Consumer {
    
    protected byte[] dictionary;
    protected SubstringPacker packer;
    private int maxDictionaryLength;

    public static CompressionModel instantiateCompressionModel(String modelName) {
        if (modelName.indexOf('.') == -1) {
            modelName = OptimizingCompressionModel.class.getPackage().getName() + "." + modelName;
            if (!modelName.endsWith("CompressionModel")) {
                modelName += "CompressionModel";
            }
        }

        CompressionModel model = null;

        try {
            Class<?> cls = Class.forName(modelName);
            model = (CompressionModel)cls.newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
         return model;
    }
    
    public void setDictionary(byte[] dictionary) {
        if (maxDictionaryLength > 0 && dictionary.length > maxDictionaryLength) {
            // We chop off the front as important strings are packed towards the end for shorter lengths/offsets
            dictionary = Arrays.copyOfRange(dictionary, dictionary.length - maxDictionaryLength, dictionary.length);
        }
        this.dictionary = dictionary;
        packer = null;
    }
    
    public byte[] getDictionary() {
        return dictionary;
    }
    
    public int getMaxDictionaryLength() {
        return maxDictionaryLength;
    }
    
    public void setMaxDictionaryLength(int length) {
        maxDictionaryLength = length;
    }
    
    protected SubstringPacker getSubstringPacker() {
        if (packer == null) {
            packer = new SubstringPacker(getDictionary());
        }
        return packer;
    }
    
    public void load(DataInputStream in) throws IOException {
        in.readInt(); // file format version, currently unused.
        
        int dictionaryLength = in.readInt();
        
        if (dictionaryLength == -1) {
            setDictionary(null);
        }
        else {
            byte[] dictionary = new byte[dictionaryLength];
            int totalRead = StreamUtil.readBytes(in, dictionary, dictionaryLength);
            if (totalRead != dictionaryLength) {
                throw new IOException("Bad model in stream.  Could not read dictionary of length " + dictionaryLength);
            }

            setDictionary(dictionary);
        }
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(0); // Poor mans file format version
        if (dictionary == null) {
            out.writeInt(-1);
        }
        else {
            out.writeInt(dictionary.length);
            out.write(dictionary);
        }
    }
    
    public static CompressionModel loadModel(String path) throws IOException {
        FileInputStream fileIn = new FileInputStream(path);
        BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
        DataInputStream in = new DataInputStream(bufferedIn);
        
        CompressionModel model = instantiateCompressionModel(in.readUTF());
        model.load(in);
        
        in.close();
        return model;
    }
    
    public void save(String path) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(path);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
        DataOutputStream out = new DataOutputStream(bufferedOut);
        
        out.writeUTF(getClass().getName());
        
        save(out);
        
        out.close();
    }
    
    public abstract void build(DocumentList documents) throws IOException;
    
    public byte[] compress(byte[] data) {
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            compress(data, bytesOut);
            return bytesOut.toByteArray();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    public void compress(byte[] data, OutputStream out) throws IOException {
        getSubstringPacker().pack(data, this, null);
    }
    
    public abstract byte[] decompress(byte[] compressedData);
    
    protected void buildDictionaryIfUnspecified(DocumentList documents) throws IOException {
        if (dictionary == null) {
            DictionaryOptimizer optimizer = new DictionaryOptimizer(documents);
            dictionary = optimizer.optimize(64*1024);
        }
    }
    
    protected SubstringPacker.Consumer createModelBuilder() {
        return null;
    }
    
    protected SubstringPacker.Consumer buildEncodingModel(DocumentList documents) {
        try {
            SubstringPacker modelBuildingPacker = new SubstringPacker(dictionary);
            SubstringPacker.Consumer modelBuilder = createModelBuilder();
            for (int i = 0, count = documents.size(); i < count; i++) {
                modelBuildingPacker.pack(documents.get(i), modelBuilder, null);
            }
            
            return modelBuilder;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

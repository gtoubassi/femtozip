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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.GZipCompressionModel;
import org.toubassi.femtozip.models.GZipDictionaryCompressionModel;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;
import org.toubassi.femtozip.models.PureHuffmanCompressionModel;
import org.toubassi.femtozip.models.VariableIntCompressionModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.util.StreamUtil;

public abstract class CompressionModel implements SubstringPacker.Consumer {
    
    protected byte[] dictionary;
    protected SubstringPacker packer;
    private int maxDictionaryLength;

    public static CompressionModel instantiateCompressionModel(String modelName) {
        if (modelName.indexOf('.') == -1) {
            modelName = FemtoZipCompressionModel.class.getPackage().getName() + "." + modelName;
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
    
    public static class ModelOptimizationResult implements Comparable<ModelOptimizationResult>{
        public CompressionModel model;
        public int totalCompressedSize;
        public int totalDataSize;

        public ModelOptimizationResult(CompressionModel model) {
            this.model = model;
        }

        public int compareTo(ModelOptimizationResult other) {
            return totalCompressedSize - other.totalCompressedSize;
        }
        
        public void accumulate(ModelOptimizationResult result) {
            totalCompressedSize += result.totalCompressedSize < result.totalDataSize ? result.totalCompressedSize:  result.totalDataSize;
            totalDataSize += result.totalDataSize;
        }
        
        public String toString() {
            DecimalFormat format = new DecimalFormat("#.##");
            String prefix = "";
            if (model != null) {
                prefix = model.getClass().getSimpleName() + " ";
            }
            return prefix + format.format((100f * totalCompressedSize) / totalDataSize) + "% (" + totalCompressedSize + " from " + totalDataSize + " bytes)";
        }
    }
    
    
    public static CompressionModel buildOptimalModel(DocumentList documents, ArrayList<ModelOptimizationResult> results, CompressionModel[] competingModels, boolean verify) throws IOException {
        
        if (competingModels == null || competingModels.length == 0) {
            competingModels = new CompressionModel[5];
            competingModels[0] = new FemtoZipCompressionModel();
            competingModels[1] = new PureHuffmanCompressionModel();
            competingModels[2] = new GZipCompressionModel();
            competingModels[3] = new GZipDictionaryCompressionModel();
            competingModels[4] = new VariableIntCompressionModel();
        }

        for (CompressionModel model : competingModels) {
            results.add(new ModelOptimizationResult(model));
        }
        
        // Split the documents into two groups.  One for building each model out
        // and one for testing which model is best.  Shouldn't build and test
        // with the same set as a model may over optimize for the training set.
        SamplingDocumentList trainingDocuments = new SamplingDocumentList(documents, 2, 0);
        SamplingDocumentList testingDocuments = new SamplingDocumentList(documents, 2, 1);
        
        // Build the dictionary once to avoid rebuilding for each model.
        byte[] dictionary = buildDictionary(trainingDocuments);

        // Build each model out
        for (ModelOptimizationResult result : results) {
            result.model.setDictionary(dictionary);
            result.model.build(trainingDocuments);
        }

        // Pick the best model

        for (int i = 0, count = testingDocuments.size(); i < count; i++) {
            byte[] data = testingDocuments.get(i);
            
            for (ModelOptimizationResult result : results) {
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                result.model.compress(data, bytesOut);
                
                if (verify) {
                    byte[] decompressed = result.model.decompress(bytesOut.toByteArray());
                    if (!Arrays.equals(data, decompressed)) {
                        throw new RuntimeException("Compress/Decompress round trip failed for " + result.model.getClass().getSimpleName());
                    }
                }
                
                result.totalCompressedSize += bytesOut.size();
                result.totalDataSize += data.length;
            }
        }
        
        Collections.sort(results);
        
        ModelOptimizationResult bestResult = results.get(0);
        return bestResult.model;
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
            dictionary = buildDictionary(documents);
        }
    }
    
    protected static byte[] buildDictionary(DocumentList documents) throws IOException {
        DictionaryOptimizer optimizer = new DictionaryOptimizer(documents);
        return optimizer.optimize(64*1024);
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

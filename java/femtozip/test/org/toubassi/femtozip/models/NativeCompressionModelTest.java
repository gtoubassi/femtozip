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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionTest;


public class NativeCompressionModelTest {
    
    @Test
    public void testNativeModel() throws IOException {
        NativeCompressionModel model = new NativeCompressionModel();

        CompressionTest.testModel(CompressionTest.PreambleString, CompressionTest.PreambleDictionary, model, 187);
        
        File modelFile = File.createTempFile("native", ".fzm");
        
        model.save(modelFile.getPath());
        model = new NativeCompressionModel();
        model.load(modelFile.getPath());

        CompressionTest.testModel(CompressionTest.PreambleString, CompressionTest.PreambleDictionary, model, 187);

        modelFile.delete();
    }
    
    
    @Test
    public void testNativeModel2() throws IOException {
        
        // Generate sample documents to train
        ArrayList<byte[]> trainingDocs = new ArrayList<byte[]>();
        for (int i = 0; i < 10; i++) {
            trainingDocs.add(generateSampleDoc((int)(Math.random() * 100) + 100));
        }

        NativeCompressionModel model = new NativeCompressionModel();
        model.build(new ArrayDocumentList(trainingDocs));
        
        for (int i = 0; i < 100; i++) {
            byte[] doc = generateSampleDoc((int)(Math.random() * 100) + 100);
            
            CompressionTest.testBuiltModel(model, doc, -1);
        }
    }
    
    private byte[] generateSampleDoc(int length) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        for (int i = 0; out.size() < length; i++) {
            if (Math.random() < .1) {
                out.write(0);
                out.write(1);
                out.write(2);
                out.write(3);
                out.write(4);
                out.write(5);
            }
            else {
                out.write((int)(Math.random() * 0xff));
            }
        }
        
        return out.toByteArray();
    }

}

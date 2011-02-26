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
package org.toubassi.femtozip.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.Tool;
import org.toubassi.femtozip.models.OptimizingCompressionModel;
import org.toubassi.femtozip.models.OptimizingCompressionModel.CompressionResult;
import org.toubassi.femtozip.util.FileUtil;

public class IndexAnalyzer extends Tool  {
    
    private HashMap<String, CompressionModel> fieldToModel = new HashMap<String, CompressionModel>();
    
    protected void buildModel() throws IOException {
        
        IndexReader reader = IndexReader.open(path);

        Collection allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);
        
        ArrayList<OptimizingCompressionModel.CompressionResult> aggregateResults = new ArrayList<OptimizingCompressionModel.CompressionResult>();
        for (String fieldName : fieldNames) {
            IndexDocumentList trainingDocs = new IndexDocumentList(reader, numSamples, 0, fieldName);
            IndexDocumentList testingDocs = new IndexDocumentList(reader, numSamples, 1, fieldName);
            
            if (trainingDocs.size() == 0 || testingDocs.size() == 0) {
                continue;
            }
            
            System.out.println("Processing field " + fieldName);
            OptimizingCompressionModel optimizingModel = (OptimizingCompressionModel)buildModel(trainingDocs, testingDocs);
            optimizingModel.aggregateResults(aggregateResults);
            fieldToModel.put(fieldName, optimizingModel);
        }        
        
        reader.close();

        OptimizingCompressionModel.CompressionResult bestResult = new CompressionResult(new OptimizingCompressionModel());
        for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
            OptimizingCompressionModel model = (OptimizingCompressionModel)entry.getValue();
            bestResult.accumulate(model.getBestPerformingResult());
        }
        
        System.out.println("Aggregate performance:");
        System.out.println(bestResult);
        Collections.sort(aggregateResults);
        for (CompressionResult result : aggregateResults) {
            System.out.println(result);
        }
    }
    
    protected void benchmarkModel() throws IOException {
        IndexReader reader = IndexReader.open(path);
        
        long totalDataSize = 0;
        long totalCompressedSize = 0;
        
        for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
            String fieldName = entry.getKey();
            CompressionModel model = entry.getValue();
            
            IndexDocumentList docs = new IndexDocumentList(reader, numSamples, 2, fieldName);

            if (docs.size() == 0) {
                continue;
            }
            
            System.out.println("Processing field " + fieldName);
            
            long[] dataSize = new long[1];
            long[] compressedSize = new long[1];
            benchmarkModel(model, docs, dataSize, compressedSize);
            totalDataSize += dataSize[0];
            totalCompressedSize += compressedSize[0];
        }
        
        reader.close();
                    
        long totalIndexSize = FileUtil.computeSize(new File(path));

        System.out.println("Summary:");
        System.out.println("Total Index Size: " + totalIndexSize);
        int numDocs = reader.numDocs();
        System.out.println("# Documents in Index: " + numDocs);
        long totalStoredDataSize = Math.round(((double)totalDataSize) * numDocs / numSamples);
        System.out.println("Estimated Stored Data Size: " + totalStoredDataSize + " (" + format.format(totalStoredDataSize * 100f / totalIndexSize) + "% of index)");
        System.out.println("Aggregate Stored Data Compression Rate: " + format.format(totalCompressedSize * 100d / totalDataSize) + "% (" + totalCompressedSize + " bytes)");
    }

    protected void loadBenchmarkModel() throws IOException {
        File modelDir = new File(modelPath);
        File[] dirContents = modelDir.listFiles();
        for (File file : dirContents) {
            if (file.getName().endsWith(".fzmodel")) {
                String fieldName = file.getName().replace(".fzmodel", "");
                CompressionModel model = CompressionModel.load(file.getPath());
                fieldToModel.put(fieldName, model);
            }
        }        
    }
    
    protected void saveBenchmarkModel() throws IOException {
        File modelDir = new File(modelPath);
        modelDir.mkdirs();
        
        // Clean up old model if we are overwriting
        File[] dirContents = modelDir.listFiles();
        for (File file : dirContents) {
            if (!file.isHidden()) {
                if (!file.delete()) {
                    throw new IOException("Could not delete " + file.getPath());
                }
            }
        }
        
        for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
            String path = modelDir.getPath() + File.separator + entry.getKey()+ ".fzmodel";
            OptimizingCompressionModel model = (OptimizingCompressionModel)entry.getValue();
            model.getBestPerformingModel().save(path);
        }
    }

    public void run(String[] args) throws IOException {
        super.run(args);
        if (operation != Operation.Benchmark && operation != Operation.BuildModel) {
            usage();
        }
    }
    
    protected void usage() {
        System.out.println("Usage: [--buildmodel|--benchmark] --modelpath path --models [Model1,Model2,...] --numsamples number indexpath");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        IndexAnalyzer analyzer = new IndexAnalyzer();
        analyzer.run(args);
    }
    
}

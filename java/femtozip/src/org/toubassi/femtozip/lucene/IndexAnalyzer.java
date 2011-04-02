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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.Tool;
import org.toubassi.femtozip.util.FileUtil;

public class IndexAnalyzer extends Tool  {
    
    private HashMap<String, CompressionModel> fieldToModel = new HashMap<String, CompressionModel>();
    private List<String> fields;
    private long totalIndexSize;
    private int totalNumDocs;
    
    private IndexReader openIndex(String path) throws IOException {
        IndexReader reader = IndexReader.open(path);
        
        totalIndexSize = FileUtil.computeSize(new File(path));
        totalNumDocs = reader.numDocs();

        return reader;
    }
    
    protected void buildModel() throws IOException {
        
        IndexReader reader = openIndex(path);

        Collection allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);

        ArrayList<CompressionModel.ModelOptimizationResult> aggregateResults = new ArrayList<CompressionModel.ModelOptimizationResult>();
        CompressionModel.ModelOptimizationResult bestResult = new CompressionModel.ModelOptimizationResult(null);
        long totalDataSize = 0;

        for (String fieldName : fieldNames) {
            
            if (fields != null && !fields.contains(fieldName)) {
                continue;
            }
            
            IndexDocumentList documents = new IndexDocumentList(reader, 2*numSamples, 0, fieldName);
            
            if (documents.size() == 0) {
                continue;
            }

            System.out.println("Processing field " + fieldName + " (containing " + documents.size() + " stored fields for " + numSamples + " documents)");
            
            ArrayList<CompressionModel.ModelOptimizationResult> results = new ArrayList<CompressionModel.ModelOptimizationResult>();
            CompressionModel model = buildModel(documents, results);
            for (CompressionModel.ModelOptimizationResult result : results) {
                CompressionModel.ModelOptimizationResult aggregateResult = null;
                for (int i = 0, count = aggregateResults.size(); i < count; i++) {
                    if (aggregateResults.get(i).model.getClass() == result.model.getClass()) {
                        aggregateResult = aggregateResults.get(i);
                        break;
                    }
                }
                if (aggregateResult == null) {
                    aggregateResult = new CompressionModel.ModelOptimizationResult(result.model);
                    aggregateResults.add(aggregateResult);
                }
                aggregateResult.accumulate(result);
            }
            
            bestResult.accumulate(results.get(0));
            totalDataSize += results.get(0).totalDataSize;
            fieldToModel.put(fieldName, model);
        }        
        
        reader.close();

        System.out.println("Summary:");
        System.out.println("Total Index Size: " + totalIndexSize);
        System.out.println("# Documents in Index: " + totalNumDocs);
        long totalStoredDataSize = Math.round(((double)totalDataSize) * totalNumDocs / numSamples);
        System.out.println("Approx. Stored Data Size: " + totalStoredDataSize + " (" + format.format(totalStoredDataSize * 100f / totalIndexSize) + "% of index)");
        
        System.out.println("Aggregate performance:");
        System.out.println("Best per Field " + bestResult);
        Collections.sort(aggregateResults);
        for (CompressionModel.ModelOptimizationResult result : aggregateResults) {
            System.out.println(result);
        }
    }
    
    protected void benchmarkModel() throws IOException {
        IndexReader reader = openIndex(path);
        
        long totalDataSize = 0;
        long totalCompressedSize = 0;
        
        for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
            String fieldName = entry.getKey();
            CompressionModel model = entry.getValue();
            
            if (fields != null && !fields.contains(fieldName)) {
                continue;
            }
            
            IndexDocumentList docs = new IndexDocumentList(reader, numSamples, 2, fieldName);

            if (docs.size() == 0) {
                continue;
            }
            
            System.out.println("Processing field " + fieldName + " (containing " + docs.size() + " stored fields for " + numSamples + " documents)");
            
            long[] dataSize = new long[1];
            long[] compressedSize = new long[1];
            benchmarkModel(model, docs, dataSize, compressedSize);
            totalDataSize += dataSize[0];
            totalCompressedSize += compressedSize[0];
        }
        
        reader.close();
                    
        System.out.println("Summary:");
        System.out.println("Total Index Size: " + totalIndexSize);
        System.out.println("# Documents in Index: " + totalNumDocs);
        long totalStoredDataSize = Math.round(((double)totalDataSize) * totalNumDocs / numSamples);
        System.out.println("Approx. Stored Data Size: " + totalStoredDataSize + " (" + format.format(totalStoredDataSize * 100f / totalIndexSize) + "% of index)");
        System.out.println("Aggregate Stored Data Compression Rate: " + format.format(totalCompressedSize * 100d / totalDataSize) + "% (" + totalCompressedSize + " bytes)");
    }

    protected void loadBenchmarkModel() throws IOException {
        File modelDir = new File(modelPath);
        File[] dirContents = modelDir.listFiles();
        for (File file : dirContents) {
            if (file.getName().endsWith(".fzmodel")) {
                String fieldName = file.getName().replace(".fzmodel", "");
                CompressionModel model = CompressionModel.loadModel(file.getPath());
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
            entry.getValue().save(path);
        }
    }

    public void run(String[] args) throws IOException {
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.equals("--fields")) {
                fields = Arrays.asList(args[++i].split(","));
            }
        }
        
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

package org.toubassi.femtozip.lucene;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.toubassi.femtozip.AbstractCompressionModel;
import org.toubassi.femtozip.models.OptimizingCompressionModel;
import org.toubassi.femtozip.models.OptimizingCompressionModel.CompressionResult;
import org.toubassi.femtozip.util.FileUtil;

public class IndexAnalyzer  {
    
    private enum Operation {
        BuildModel, Benchmark
    }

    private DecimalFormat format = new DecimalFormat("#.##");

    private Operation operation;
    
    private String indexPath;
    private String modelPath;
    private String[] models;
    
    private HashMap<String, AbstractCompressionModel> fieldToModel = new HashMap<String, AbstractCompressionModel>();
    
    private int numSamples = 0;
    private int maxDictionarySize = 0;
    
    protected void buildModel(IndexReader reader) throws IOException {
        Collection allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);

        ArrayList<OptimizingCompressionModel.CompressionResult> aggregateResults = new ArrayList<OptimizingCompressionModel.CompressionResult>();
        for (String fieldName : fieldNames) {
            long start = System.currentTimeMillis();
            
            IndexDocumentList trainingDocs = new IndexDocumentList(reader, numSamples, 0, fieldName);
            IndexDocumentList testingDocs = new IndexDocumentList(reader, numSamples, 1, fieldName);
            
            if (trainingDocs.size() == 0 || testingDocs.size() == 0) {
                continue;
            }
            
            OptimizingCompressionModel model = models == null ? new OptimizingCompressionModel() : new OptimizingCompressionModel(models);
            fieldToModel.put(fieldName, model);
            
            System.out.print("Building model for " + fieldName);
            
            model.build(trainingDocs);
            model.optimize(testingDocs);
            
            long duration = Math.round((System.currentTimeMillis() - start)/1000d);
            System.out.println(" (" + duration + "s)");
            model.dump();
            System.out.println();
            model.aggregateResults(aggregateResults);
        }

        OptimizingCompressionModel.CompressionResult bestResult = new CompressionResult(new OptimizingCompressionModel());
        for (Map.Entry<String, AbstractCompressionModel> entry : fieldToModel.entrySet()) {
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
    
    protected void benchmarkModel(IndexReader reader, long totalDataSize[], long totalCompressedSize[]) throws IOException {
        for (Map.Entry<String, AbstractCompressionModel> entry : fieldToModel.entrySet()) {
            String fieldName = entry.getKey();
            AbstractCompressionModel model = entry.getValue();
            
            long start = System.currentTimeMillis();
            
            IndexDocumentList docs = new IndexDocumentList(reader, numSamples, 2, fieldName);

            if (docs.size() == 0) {
                continue;
            }
            
            System.out.print("Benchmarking " + model.getClass().getSimpleName() + " for " + fieldName);
            
            int dataSize = 0;
            int compressedSize = 0;
            for (int i = 0, count = docs.size(); i < count; i++) {
                byte[] bytes = docs.get(i);
                
                byte[] compressed = model.compress(bytes);
                dataSize += bytes.length;
                compressedSize += compressed.length;
                
                if (true) {
                    byte[] decompressed = model.decompress(compressed);
                    if (!Arrays.equals(bytes, decompressed)) {
                        throw new RuntimeException("Compress/Decompress round trip failed for " + model.getClass().getSimpleName());
                    }
                }
            }
            
            totalDataSize[0] += dataSize;
            totalCompressedSize[0] += compressedSize;
            
            String duration = format.format((System.currentTimeMillis() - start)/1000f);
            String ratio = format.format(100f * compressedSize / dataSize);
            System.out.println(" in " + duration + "s:  " + ratio  + "% (" + compressedSize + "/" + dataSize + ")");
        }
        
    }
    
    protected void loadBenchmarkModel() throws IOException {
        File modelDir = new File(modelPath);
        File[] dirContents = modelDir.listFiles();
        for (File file : dirContents) {
            if (file.getName().endsWith(".fzmodel")) {
                String fieldName = file.getName().replace(".fzmodel", "");
                AbstractCompressionModel model = AbstractCompressionModel.load(file.getPath());
                fieldToModel.put(fieldName, model);
            }
        }        
    }
    
    protected OptimizingCompressionModel createModel() {
        OptimizingCompressionModel model = new OptimizingCompressionModel(models);
        if (maxDictionarySize > 0) {
            model.setMaxDictionaryLength(maxDictionarySize);
        }
        return model;
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
        
        for (Map.Entry<String, AbstractCompressionModel> entry : fieldToModel.entrySet()) {
            String path = modelDir.getPath() + File.separator + entry.getKey()+ ".fzmodel";
            OptimizingCompressionModel model = (OptimizingCompressionModel)entry.getValue();
            model.getBestPerformingModel().save(path);
        }
    }
    
    protected static void usage() {
        System.out.println("Usage: [--buildmodel|--benchmark] --modelpath path --models [Model1,Model2,...] --numsamples number indexpath");
        System.exit(1);
    }
    
    public void run(String[] args) throws IOException {
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.equals("--benchmark")) {
                operation = Operation.Benchmark;
            }
            else if (arg.equals("--buildmodel")) {
                operation = Operation.BuildModel;
            }
            else if (arg.equals("--numsamples")) {
                numSamples = Integer.parseInt(args[++i]);
            }
            else if (arg.equals("--modelpath")) {
                modelPath = args[++i];
            }
            else if (arg.equals("--models")) {
                models = args[++i].split(",");
            }
            else if (arg.equals("--maxdict")) {
                maxDictionarySize = Integer.parseInt(args[++i]);
            }
            else {
                indexPath = arg;
            }
        }
        
        if (operation == null || indexPath == null || modelPath == null) {
            usage();
        }

        IndexReader reader = IndexReader.open(indexPath);

        if (operation == Operation.BuildModel) {
            buildModel(reader);
            saveBenchmarkModel();
        }        
        else if (operation == Operation.Benchmark) {
            loadBenchmarkModel();
            
            long[] totalDataSizeRef = new long[1];
            long[] totalCompressedSizeRef = new long[1];
            benchmarkModel(reader, totalDataSizeRef, totalCompressedSizeRef);
            long totalDataSize = totalDataSizeRef[0];
            long totalCompressedSize = totalCompressedSizeRef[0];
            
            long totalIndexSize = FileUtil.computeSize(new File(indexPath));

            System.out.println("Summary:");
            System.out.println("Total Index Size: " + totalIndexSize);
            int numDocs = reader.numDocs();
            System.out.println("# Documents in Index: " + numDocs);
            long totalStoredDataSize = Math.round(((double)totalDataSize) * numDocs / numSamples);
            System.out.println("Estimated Stored Data Size: " + totalStoredDataSize + " (" + format.format(totalStoredDataSize * 100f / totalIndexSize) + "% of index)");
            System.out.println("Aggregate Stored Data Compression Rate: " + format.format(totalCompressedSize * 100d / totalDataSize) + "% (" + totalCompressedSize + " bytes)");
        }
        
        reader.close();
    }
    
    public static void main(String[] args) throws IOException {
        IndexAnalyzer analyzer = new IndexAnalyzer();
        analyzer.run(args);
    }
    
}

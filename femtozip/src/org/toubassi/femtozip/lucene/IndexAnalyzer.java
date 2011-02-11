package org.toubassi.femtozip.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.encoding.benchmark.BenchmarkEncodingModel;

public class IndexAnalyzer  {
    
    private enum Operation {
        BuildModel, Benchmark
    }

    private Operation operation;
    
    private String indexPath;
    private String modelPath;
    private String[] encodings;
    
    private HashMap<String, CompressionModel> fieldToModel = new HashMap<String, CompressionModel>();
    
    private int numSamples = 0;
    private long totalIndexSize = 0;
    private int numDocs = 0;
    private int maxDictionarySize = 0;
    
    private long computeSize(File root) {
        File[] files = root.listFiles();
        
        long size = 0;
        for (File subFile : files) {
            if (!subFile.getPath().endsWith(".fzmodel")) {
                size += subFile.length();
                if (subFile.isDirectory()) {
                    size += computeSize(subFile);
                }
            }
        }
        
        return size;
    }
    
    protected void analyze() throws IOException {
        totalIndexSize = computeSize(new File(indexPath));
        IndexReader reader = IndexReader.open(indexPath);
        
        Collection allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);
        
        numDocs = reader.numDocs();
        int maxDocId = reader.maxDoc();
        float samplingRate = ((float)numSamples) / numDocs;

        int numDocsScanned = 0;
        int numDocsSampled = 0;
        long lastStatus = 0;
        for (int docId = 0; docId < maxDocId; docId++) {
            
            if (reader.isDeleted(docId)) {
                continue;
            }
            
            numDocsScanned++;
            
            if (((int)(numDocsScanned * samplingRate)) <= numDocsSampled) {
                continue;
            }
            
            numDocsSampled++;

            if (System.currentTimeMillis() - lastStatus > 5000) {
                System.out.println("Sampling doc id " + docId + " (" + numDocsSampled + " of " + numSamples + ")");
                lastStatus = System.currentTimeMillis();
            }
            
            Document doc = reader.document(docId);
            
            for (String fieldName : fieldNames) {
                
                Field[] fields = doc.getFields(fieldName);
                
                for (Field field : fields) {
                    
                    if (!field.isStored() || field.isCompressed()) {
                        // TODO if its compressed, uncompress it and benchmark it.
                        continue;
                    }
                    
                    byte[] bytes;
                    
                    if (field.isBinary()) {
                        bytes = new byte[field.getBinaryLength()];
                        System.arraycopy(field.getBinaryValue(), field.getBinaryOffset(), bytes, 0, field.getBinaryLength());
                    }
                    else {
                        String value = field.stringValue();
                        bytes = value.getBytes("UTF-8");
                    }
                    
                    if (bytes.length > 0) {
                        CompressionModel model = fieldToModel.get(fieldName);
                        
                        if (operation == Operation.BuildModel) {
                            if (model == null) {
                                model = createModel();
                                fieldToModel.put(fieldName, model);
                                if (operation == Operation.BuildModel) {
                                    model.beginModelConstruction();
                                }
                            }
                            model.addDocumentToModel(bytes);
                        }
                        else if (operation == Operation.Benchmark) {
                            if (model == null) {
                                System.err.println("WARNING: No model for field " + fieldName + ".  Either corrupt model or that field was not sampled when the model was built.  Skipping");
                            }
                            else {
                                try {
                                    model.compress(bytes);
                                }
                                catch (RuntimeException e) {
                                    System.err.println("Caught exception processing document " + docId + " field " + fieldName);
                                    throw e;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (operation == Operation.BuildModel) {
            for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
                System.out.print("Building model for " + entry.getKey());
                long start = System.currentTimeMillis();
                entry.getValue().endModelConstruction();
                long duration = Math.round((System.currentTimeMillis() - start)/1000d);
                System.out.println(" (" + duration + "s)");
            }
        }
        
        reader.close();
    }
    
    protected void loadBenchmarkModel() throws IOException {
        File modelDir = new File(modelPath);
        File[] dirContents = modelDir.listFiles();
        for (File file : dirContents) {
            if (file.getName().endsWith(".fzmodel")) {
                String fieldName = file.getName().replace(".fzmodel", "");
                CompressionModel model = createModel();
                model.load(file.getPath());
                fieldToModel.put(fieldName, model);
            }
        }        
    }
    
    protected CompressionModel createModel() {
        CompressionModel model = new CompressionModel(new BenchmarkEncodingModel(encodings));
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
        
        for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
            String path = modelDir.getPath() + File.separator + entry.getKey()+ ".fzmodel";
            entry.getValue().save(path);
            if (true) {
                entry.getValue().dump(path + "-diagnostics");
            }
        }
    }
    
    protected static void usage() {
        System.out.println("Usage: [--buildmodel|--benchmark] --modelpath path --encodings [Encoding1,Encoding2,Encoding3] --numsamples number indexpath");
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
            else if (arg.equals("--encodings")) {
                encodings = args[++i].split(",");
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
        
        if (operation == Operation.Benchmark) {
            loadBenchmarkModel();
        }

        analyze();

        if (operation == Operation.BuildModel) {
            saveBenchmarkModel();
        }        
        else if (operation == Operation.Benchmark) {
            long totalBytesSampled = 0;
            long totalCompressedBytes = 0;
            for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
                System.out.println("Benchmark for field " + entry.getKey() + ":");
                BenchmarkEncodingModel model = ((BenchmarkEncodingModel)entry.getValue().getEncodingModel());
                model.dump();
                totalBytesSampled += model.getTotalSize();
                totalCompressedBytes += model.getBestPerformingTotalCompressedSize();
                System.out.println();
            }
            
            System.out.println("Summary:");
            System.out.println("Total Index Size: " + totalIndexSize);
            System.out.println("# Documents in Index: " + numDocs);
            long totalStoredDataSize = Math.round(((float)totalBytesSampled) * numDocs / numSamples);
            System.out.println("Estimated Stored Data Size: " + totalStoredDataSize + " (" + Math.round(totalStoredDataSize * 100f / totalIndexSize) + "% of index)");
            System.out.println("Aggregate Stored Data Compression Rate: " + Math.round(totalCompressedBytes * 100f / totalBytesSampled) + "% (" + totalCompressedBytes + " bytes)");
            
            System.out.println("Best Performing Encodings:");
            for (Map.Entry<String, CompressionModel> entry : fieldToModel.entrySet()) {
                BenchmarkEncodingModel model = ((BenchmarkEncodingModel)entry.getValue().getEncodingModel());
                System.out.println(entry.getKey() + ": " + model.getBestPerformingEncoding() + " (" + Math.round(100f * model.getBestPerformingTotalCompressedSize() / model.getTotalSize()) + "%)");
            }
            
        }
    }
    
    public static void main(String[] args) throws IOException {
        IndexAnalyzer analyzer = new IndexAnalyzer();
        analyzer.run(args);
    }
    
}

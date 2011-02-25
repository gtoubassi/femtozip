package org.toubassi.femtozip;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.toubassi.femtozip.models.OptimizingCompressionModel;

public class Tool  {
    
    protected enum Operation {
        BuildModel, Benchmark
    }

    protected DecimalFormat format = new DecimalFormat("#.##");

    protected Operation operation;
    
    protected String path;
    protected String modelPath;
    protected String[] models;
    protected CompressionModel model;
    
    protected int numSamples = 0;
    protected int maxDictionarySize = 0;
    
    protected CompressionModel buildModel(DocumentList trainingDocs, DocumentList testingDocs) throws IOException {
        
        long start = System.currentTimeMillis();
        
        model = models == null ? new OptimizingCompressionModel() : new OptimizingCompressionModel(models);

        System.out.print("Building model...");
        
        model.build(trainingDocs);
        ((OptimizingCompressionModel)model).optimize(testingDocs);

        long duration = Math.round((System.currentTimeMillis() - start)/1000d);
        System.out.println(" (" + duration + "s)");
        ((OptimizingCompressionModel)model).dump();
        System.out.println();
        return model;
    }
    
    protected void buildModel() throws IOException {
        
        File dir = new File(path);
        List<String> files = Arrays.asList(dir.list());
        Collections.shuffle(files, new Random(1234567890)); // Avoid any bias in ordering of the files
        numSamples = Math.min(numSamples, files.size());
        FileDocumentList trainingDocs = new FileDocumentList(path, files.subList(0, numSamples));
        FileDocumentList testingDocs = new FileDocumentList(path, files.subList(files.size() - numSamples, files.size()));

        buildModel(trainingDocs, testingDocs);
    }

    protected void benchmarkModel(CompressionModel model, DocumentList docs, long totalDataSize[], long totalCompressedSize[]) throws IOException {
        System.out.print("Benchmarking " + model.getClass().getSimpleName() + " ");
        
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

        String ratio = format.format(100f * compressedSize / dataSize);
        System.out.println(ratio  + "% (" + compressedSize + "/" + dataSize + ")\n");

    }
    
    protected void benchmarkModel() throws IOException {
        File dir = new File(path);
        List<String> files = Arrays.asList(dir.list());
        Collections.shuffle(files, new Random(1234567890)); // Avoid any bias in ordering of the files
        numSamples = Math.min(numSamples, files.size());
        FileDocumentList docs = new FileDocumentList(path, files.subList(0, numSamples));

        long[] totalDataSizeRef = new long[1];
        long[] totalCompressedSizeRef = new long[1];
        benchmarkModel(model, docs, totalDataSizeRef, totalCompressedSizeRef);
        long totalCompressedSize = totalCompressedSizeRef[0];
        long totalDataSize = totalDataSizeRef[0];
        
        System.out.println("Summary:");
        System.out.println("Aggregate Stored Data Compression Rate: " + format.format(totalCompressedSize * 100d / totalDataSize) + "% (" + totalCompressedSize + " bytes)");
    }
    
    protected void loadBenchmarkModel() throws IOException {
        model = CompressionModel.load(modelPath);
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
        modelDir.getParentFile().mkdirs();
        
        ((OptimizingCompressionModel)model).getBestPerformingModel().save(modelPath);
    }
    
    protected static void usage() {
        System.out.println("Usage: [--buildmodel|--benchmark] --modelpath path --models [Model1,Model2,...] --numsamples number path");
        System.exit(1);
    }
    
    public void run(String[] args) throws IOException {
        
        System.out.println("Command line arguments:");
        for (String arg : args) {
            System.out.println(arg);
        }
        System.out.println();
        
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
                path = arg;
            }
        }
        
        if (operation == null || path == null || modelPath == null) {
            usage();
        }

        long start = System.currentTimeMillis();
        
        if (operation == Operation.BuildModel) {
            buildModel();
            saveBenchmarkModel();
        }        
        else if (operation == Operation.Benchmark) {
            loadBenchmarkModel();
            
            benchmarkModel();
        }
        
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("Took " + format.format(duration / 1000f) + "s");
    }
    
    public static void main(String[] args) throws IOException {
        Tool tool = new Tool();
        tool.run(args);
    }
    
}

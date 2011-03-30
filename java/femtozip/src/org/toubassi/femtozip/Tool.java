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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.toubassi.femtozip.models.OptimizingCompressionModel;
import org.toubassi.femtozip.util.FileUtil;

public class Tool  {
    
    protected enum Operation {
        BuildModel, Benchmark, Compress, Decompress
    }

    protected DecimalFormat format = new DecimalFormat("#.##");

    protected Operation operation;
    
    protected String path;
    protected String modelPath;
    protected String[] models;
    protected CompressionModel model;
    protected boolean preload;
    protected boolean verify;
    
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

        long compressTime = 0;
        long decompressTime = 0;
        int dataSize = 0;
        int compressedSize = 0;
        for (int i = 0, count = docs.size(); i < count; i++) {
            byte[] bytes = docs.get(i);
            
            long startCompress = System.nanoTime();
            byte[] compressed = model.compress(bytes);
            compressTime += System.nanoTime() - startCompress;

            dataSize += bytes.length;
            compressedSize += compressed.length;
            
            if (verify) {
                long startDecompress = System.nanoTime();
                byte[] decompressed = model.decompress(compressed);
                decompressTime += System.nanoTime() - startDecompress;
                if (!Arrays.equals(bytes, decompressed)) {
                    throw new RuntimeException("Compress/Decompress round trip failed for " + model.getClass().getSimpleName());
                }
            }
        }

        totalDataSize[0] += dataSize;
        totalCompressedSize[0] += compressedSize;

        decompressTime /= 1000000;
        compressTime /= 1000000;
        String ratio = format.format(100f * compressedSize / dataSize);
        System.out.println(ratio  + "% (" + compressedSize + "/" + dataSize + "  compressed:" + compressTime + "ms" + (verify ? (" decompress:" + decompressTime + "ms") : "") + ")\n");
    }
    
    protected void benchmarkModel() throws IOException {
        File dir = new File(path);
        List<String> files = Arrays.asList(dir.list());
        Collections.shuffle(files, new Random(1234567890)); // Avoid any bias in ordering of the files
        numSamples = Math.min(numSamples, files.size());
        FileDocumentList docs = new FileDocumentList(path, files.subList(0, numSamples), preload);

        long start = System.currentTimeMillis();
        long[] totalDataSizeRef = new long[1];
        long[] totalCompressedSizeRef = new long[1];
        benchmarkModel(model, docs, totalDataSizeRef, totalCompressedSizeRef);
        long totalCompressedSize = totalCompressedSizeRef[0];
        long totalDataSize = totalDataSizeRef[0];
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("Summary:");
        System.out.println("Aggregate Stored Data Compression Rate: " + format.format(totalCompressedSize * 100d / totalDataSize) + "% (" + totalCompressedSize + " bytes)");
        System.out.println("Compression took " + format.format(duration / 1000f) + "s");
    }

    protected void compress(File file) throws IOException {
        System.out.println("Compressing " + file.getName());
        byte[] data = FileUtil.readFile(file);
        byte[] compressed = model.compress(data);
        
        File outputFile = new File(file.getPath() + ".fz");
        FileOutputStream out = new FileOutputStream(outputFile);
        out.write(compressed);
        out.close();
        file.delete();
    }
    
    protected void compress() throws IOException {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                compress(f);
            }
        }
        else {
            compress(file);
        }
    }

    protected void decompress(File file) throws IOException {
        System.out.println("Decompressing " + file.getName());
        byte[] compressed = FileUtil.readFile(file);
        byte[] data = model.decompress(compressed);
        
        File outputFile = new File(file.getPath().substring(0, file.getPath().length() - 3));
        FileOutputStream out = new FileOutputStream(outputFile);
        out.write(data);
        out.close();
        file.delete();
    }
    
    protected void decompress() throws IOException {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                System.out.println(f);
                if (f.getName().endsWith(".fz")) {
                    decompress(f);
                }
            }
        }
        else {
            decompress(file);
        }
    }
    
    protected void loadBenchmarkModel() throws IOException {
        model = CompressionModel.loadModel(modelPath);
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
    
    protected void usage() {
        System.out.println("Usage: [--buildmodel|--benchmark|--compress|--decompress] --modelpath path --models [Model1,Model2,...] --numsamples number path");
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
            else if (arg.equals("--compress")) {
                operation = Operation.Compress;
            }
            else if (arg.equals("--decompress")) {
                operation = Operation.Decompress;
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
            else if (arg.equals("--preload")) {
                preload = true;
            }
            else if (arg.equals("--verify")) {
                verify = true;
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
        else if (operation == Operation.Compress) {
            loadBenchmarkModel();
            compress();
        }
        else if (operation == Operation.Decompress) {
            loadBenchmarkModel();
            decompress();
        }
        
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("Took " + format.format(duration / 1000f) + "s");
    }
    
    public static void main(String[] args) throws IOException {
        Tool tool = new Tool();
        tool.run(args);
    }
    
}

package org.toubassi.femtozip.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.EncodingModel;
import org.toubassi.femtozip.encoding.benchmark.BenchmarkEncodingModel;

public class Tool {
    
    private enum Operation {
        Compress, Decompress, BuildModel
    }

    protected Operation operation = null;
    protected String modelPath = null;
    protected String encodingClassName = "OffsetNibble";
    protected ArrayList<String> files = new ArrayList<String>();
    
    protected static void usage() {
        System.out.println("Usage: [--compress|--decompress|--buildmodel] --modelpath path [--encoding EncodingModel] document1 [document2 ...]");
        System.exit(1);
    }
    
    private void buildModel(DocumentList documentList, EncodingModel encodingModel) throws IOException {
        CompressionModel model = new CompressionModel(encodingModel);
        
        model.beginModelConstruction();
        
        for (int i = 0, count = documentList.size(); i < count; i++) {
            byte[] document = documentList.get(i);
            model.addDocumentToModel(document);
        }
        
        model.endModelConstruction();
        
        model.save(modelPath);
    }
    
    private void compress(DocumentList documentList, EncodingModel encodingModel) throws IOException {
        CompressionModel model = new CompressionModel(encodingModel);
        
        model.load(modelPath);
        
        long totalOriginalSize = 0;
        long totalCompressedSize = 0;
        
        for (int i = 0, count = documentList.size(); i < count; i++) {
            byte[] document = documentList.get(i);
            System.out.print("Compressing " + documentList.getName(i));
            
            byte[] compressedDoc = model.compress(document);
            totalOriginalSize += document.length;
            totalCompressedSize += compressedDoc.length;
            if (!(encodingModel instanceof BenchmarkEncodingModel)) {
                documentList.setCompressed(i, compressedDoc);
            }
            System.out.println(" to " + (100 * compressedDoc.length / document.length) + "%");
        }
        System.out.println(totalOriginalSize + " bytes compress to " + totalCompressedSize + " bytes " + (100 * totalCompressedSize / totalOriginalSize) + "%");
    }
    
    private void decompress(DocumentList documentList, EncodingModel encodingModel) throws IOException {
        CompressionModel model = new CompressionModel(encodingModel);

        model.load(modelPath);
        
        for (int i = 0, count = documentList.size(); i < count; i++) {
            byte[] compressedDoc = documentList.get(i);
            System.out.println("Decompressing " + documentList.getName(i));
            
            byte[] doc = model.decompress(compressedDoc);
            documentList.setDecompressed(i, doc);
        }
    }
    
    protected DocumentList createDocumentList(ArrayList<String> files) {
        return new FileDocumentList(files);
    }
    
    protected int processArgument(String[] args, int i) {
        String arg = args[i];
        
        if (arg.equals("--compress")) {
            operation = Operation.Compress;
        }
        else if (arg.equals("--decompress")) {
            operation = Operation.Decompress;
        }
        else if (arg.equals("--buildmodel")) {
            operation = Operation.BuildModel;
        }
        else if (arg.equals("--modelpath")) {
            modelPath = args[++i];
        }
        else if (arg.equals("--encoding")) {
            encodingClassName = args[++i];
        }
        
        return i;
    }
    
    protected void checkArguments() {
        if (modelPath == null) {
            usage();
        }
        if (files.size() == 0) {
            usage();
        }
        if (operation == null) {
            usage();
        }
    }
    
    public void run(String[] args) throws IOException {        
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.startsWith("--")) {
                i = processArgument(args, i);
            }
            else {
                files.add(arg);
            }
        }

        if (files.size() == 1) {
            // Running from the eclipse debugger can't glob cmd line args!?
            String file = files.get(0);
            if (file.indexOf('*') != -1) {
                files.remove(0);
                String pattern = "^" + file.replace("*", ".*") + "$";
                File parent = (new File(file)).getCanonicalFile().getParentFile();
                String[] candidateFiles = parent.list();
                for (String candidateFile : candidateFiles) {
                    if (candidateFile.matches(pattern)) {
                        files.add(candidateFile);
                    }
                }
            }
        }
        
        checkArguments();
        
        DocumentList documentList = createDocumentList(files);
        
        EncodingModel encodingModel = CompressionModel.instantiateEncodingModel(encodingClassName);
        
        if (operation == Operation.BuildModel) {
            buildModel(documentList, encodingModel);
        }
        if (operation == Operation.Compress) {
            compress(documentList, encodingModel);
        }
        if (operation == Operation.Decompress) {
            decompress(documentList, encodingModel);
        }
        
        if (encodingModel instanceof BenchmarkEncodingModel) {
            ((BenchmarkEncodingModel)encodingModel).dump();
        }
    }

    public static void main(String[] args) throws IOException {
        Tool tool = new Tool();
        tool.run(args);
    }
}

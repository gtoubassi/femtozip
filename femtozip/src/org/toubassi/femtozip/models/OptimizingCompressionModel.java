package org.toubassi.femtozip.models;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import org.toubassi.femtozip.AbstractCompressionModel;
import org.toubassi.femtozip.DocumentList;

public class OptimizingCompressionModel extends AbstractCompressionModel {

    public static class CompressionResult implements Comparable<CompressionResult>{
        public AbstractCompressionModel model;
        public int totalCompressedSize;
        public int totalDataSize;

        public CompressionResult(AbstractCompressionModel model) {
            this.model = model;
        }

        public int compareTo(CompressionResult other) {
            return totalCompressedSize - other.totalCompressedSize;
        }
        
        public void accumulate(CompressionResult result) {
            totalCompressedSize += result.totalCompressedSize < result.totalDataSize ? result.totalCompressedSize:  result.totalDataSize;
            totalDataSize += result.totalDataSize;
        }
        
        public String toString() {
            DecimalFormat format = new DecimalFormat("#.##");
            return model.getClass().getSimpleName() + " " + format.format((100f * totalCompressedSize) / totalDataSize) + "% (" + totalCompressedSize + " from " + totalDataSize + " bytes)";
        }
    }

    private ArrayList<CompressionResult> results = new ArrayList<CompressionResult>();
    private ArrayList<CompressionResult> sortedResults;
    private int totalDataSize;

    public OptimizingCompressionModel() {
        this("DeflateFrequency,GZip,GZipDictionary,NibbleFrequency,OffsetNibbleFrequency,PureArithCoding,PureHuffman,SplitFrequency,TripleNibbleFrequency,UnifiedFrequency,Noop");
    }
    
    public OptimizingCompressionModel(String modelNames) {
        this(modelNames.split(","));
    }
    
    public OptimizingCompressionModel(String[] modelNames) {
        for (String modelName : modelNames) {
            AbstractCompressionModel model = AbstractCompressionModel.instantiateCompressionModel(modelName);
            results.add(new CompressionResult(model));
        }
    }
    
    public void load(DataInputStream in) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public void save(DataOutputStream out) throws IOException {
        throw new UnsupportedOperationException("You should save the best performing model, not the OptimizingCompressionModel");
    }
    
    @Override
    public void build(DocumentList documents) throws IOException {
        buildDictionaryIfUnspecified(documents);

        for (CompressionResult result : results) {
            
            // No need to recompute this over and over.  This assumes all types of compression model
            // compute the dictionary the same way.
            result.model.setDictionary(getDictionary());
            
            result.model.build(documents);
        }
    }
    
    /**
     * Should be a different set of documents from those used to build
     */
    public void optimize(DocumentList documents) throws IOException {
        
        for (int i = 0, count = documents.size(); i < count; i++) {
            byte[] data = documents.get(i);
            
            totalDataSize += data.length;
            
            for (CompressionResult result : results) {
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                result.model.compress(data, bytesOut);
                
                result.totalCompressedSize += bytesOut.size();
                result.totalDataSize += data.length;
            }
        }
        
        sortedResults = new ArrayList<CompressionResult>(results);
        Collections.sort(sortedResults);
    }

    
    public void compress(byte[] data, OutputStream out) throws IOException {
        getBestPerformingModel().compress(data, out);
    }
    

    public byte[] decompress(byte[] compressedData) {
        return getBestPerformingModel().decompress(compressedData);
    }
    
    public AbstractCompressionModel getBestPerformingModel() {
        int best = sortedResults.get(0).totalCompressedSize;
        return best > totalDataSize ? null : sortedResults.get(0).model;
    }
    
    public CompressionResult getBestPerformingResult() {
        return sortedResults.get(0);
    }
    
    public void encodeLiteral(int aByte) {
        throw new UnsupportedOperationException();
    }

    public void encodeSubstring(int offset, int length) {
        throw new UnsupportedOperationException();
    }
    
    public void aggregateResults(ArrayList<CompressionResult> aggregateResults) {
        if (aggregateResults.size() == 0) {
            for (CompressionResult result : results) {
                aggregateResults.add(new CompressionResult(result.model));
            }
        }
        for (int i = 0, count = results.size(); i < count; i++) {
            CompressionResult result = results.get(i);
            CompressionResult aggregate = aggregateResults.get(i);
            
            if (result.model.getClass() != aggregate.model.getClass()){
                throw new RuntimeException("Can't aggregate across different sets of models");
            }
            aggregate.accumulate(result);
        }
    }
    
    public void dump() {
        if (totalDataSize <= 0) {
            return;
        }
        
        
        for (CompressionResult result : sortedResults) {
            System.out.println(result);
        }
    }
}

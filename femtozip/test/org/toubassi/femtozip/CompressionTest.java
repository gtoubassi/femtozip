package org.toubassi.femtozip;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.encoding.deflatefrequency.DeflateFrequencyEncodingModel;
import org.toubassi.femtozip.encoding.nibblefrequency.NibbleFrequencyEncodingModel;
import org.toubassi.femtozip.encoding.offsetnibblefrequency.OffsetNibbleFrequencyEncodingModel;
import org.toubassi.femtozip.encoding.splitfrequency.SplitFrequencyEncodingModel;
import org.toubassi.femtozip.encoding.triplenibblefrequency.TripleNibbleFrequencyEncodingModel;
import org.toubassi.femtozip.encoding.unifiedfrequency.UnifiedFrequencyEncodingModel;
import org.toubassi.femtozip.encoding.verbosestring.VerboseStringEncodingModel;


public class CompressionTest {
    private static String PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
    private String PreambleDictionary = " of and for the a United States ";

    private static String PanamaString = "a man a plan a canal panama";
    
    
    @Test
    public void testDictionaryOptimizer() throws IOException {
        
        CompressionModel compressionModel = new CompressionModel(new UnifiedFrequencyEncodingModel());
        compressionModel.beginModelConstruction();
        compressionModel.addDocumentToModel(PreambleString.getBytes());
        compressionModel.endModelConstruction();
        
        String dictionary = dictionaryToString(compressionModel.getDictionary());
        Assert.assertEquals(" our to , ince, sticure , proity, s of  and  for the  establish e the  the United States", dictionary);
        
        compressionModel = new CompressionModel(new UnifiedFrequencyEncodingModel());
        compressionModel.beginModelConstruction();
        compressionModel.addDocumentToModel(PanamaString.getBytes());
        compressionModel.endModelConstruction();
        
        dictionary = dictionaryToString(compressionModel.getDictionary());
        Assert.assertEquals("an a ", dictionary);
    }
    
    
    @Test
    public void testEncodingModels() throws IOException {
        testModel(PreambleString, PreambleDictionary, new VerboseStringEncodingModel());
        testModel(PreambleString, PreambleDictionary, new UnifiedFrequencyEncodingModel());
        testModel(PreambleString, PreambleDictionary, new SplitFrequencyEncodingModel());
        testModel(PreambleString, PreambleDictionary, new OffsetNibbleFrequencyEncodingModel());
        testModel(PreambleString, PreambleDictionary, new NibbleFrequencyEncodingModel());
        testModel(PreambleString, PreambleDictionary, new DeflateFrequencyEncodingModel());
        testModel(PreambleString, PreambleDictionary, new TripleNibbleFrequencyEncodingModel());
        // Pending: test PureSymbol and PureHuffman
    }
    
    private static String dictionaryToString(byte[] dictionary) {
        int i = 0, count;
        for (i = 0, count = dictionary.length; i < count && dictionary[i] == 0; i++) {
        }
        return new String(Arrays.copyOfRange(dictionary, i, dictionary.length));
    }
    
    private void testModel(String source, String dictionary, EncodingModel encodingModel) throws IOException {
        byte[] sourceBytes = source.getBytes();
        byte[] dictionaryBytes = dictionary == null ? null : dictionary.getBytes();
        
        CompressionModel compressionModel = new CompressionModel(encodingModel);
        compressionModel.setDictionary(dictionaryBytes);
        compressionModel.beginModelConstruction();
        compressionModel.addDocumentToModel(sourceBytes);
        compressionModel.endModelConstruction();
        
        byte[] compressedBytes = compressionModel.compress(sourceBytes);
        
        System.out.println(sourceBytes.length + " compressed to " + compressedBytes.length + " using " + encodingModel.getClass().getSimpleName());

        byte[] decompressedBytes = compressionModel.decompress(compressedBytes);
        String decompressedString = new String(decompressedBytes);
        
        Assert.assertEquals(source, decompressedString);
    }
}

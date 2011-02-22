package org.toubassi.femtozip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.models.DeflateFrequencyCompressionModel;
import org.toubassi.femtozip.models.GZipCompressionModel;
import org.toubassi.femtozip.models.GZipDictionaryCompressionModel;
import org.toubassi.femtozip.models.NibbleFrequencyCompressionModel;
import org.toubassi.femtozip.models.NoopCompressionModel;
import org.toubassi.femtozip.models.OffsetNibbleFrequencyCompressionModel;
import org.toubassi.femtozip.models.PureArithCodingCompressionModel;
import org.toubassi.femtozip.models.PureHuffmanCompressionModel;
import org.toubassi.femtozip.models.SplitFrequencyCompressionModel;
import org.toubassi.femtozip.models.TripleNibbleFrequencyCompressionModel;
import org.toubassi.femtozip.models.UnifiedFrequencyCompressionModel;
import org.toubassi.femtozip.models.VerboseStringCompressionModel;


public class CompressionTest {
    private static String PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
    private String PreambleDictionary = " of and for the a United States ";

    private static String PanamaString = "a man a plan a canal panama";
    
    
    @Test
    public void testDictionaryOptimizer() throws IOException {
        
        CompressionModel compressionModel = new UnifiedFrequencyCompressionModel();
        compressionModel.build(new ArrayDocumentList(PreambleString.getBytes()));

        String dictionary = dictionaryToString(compressionModel.getDictionary());
        Assert.assertEquals(" our to , ince, sticure and , proity, s of e the for the establish the United States", dictionary);
        
        compressionModel = new UnifiedFrequencyCompressionModel();
        compressionModel.build(new ArrayDocumentList(PanamaString.getBytes()));
        
        dictionary = dictionaryToString(compressionModel.getDictionary());
        Assert.assertEquals("an a ", dictionary);
    }
    
    
    @Test
    public void testCompressionModels() throws IOException {
        testModel2(PreambleString, PreambleDictionary, new VerboseStringCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new UnifiedFrequencyCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new SplitFrequencyCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new NibbleFrequencyCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new TripleNibbleFrequencyCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new OffsetNibbleFrequencyCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new DeflateFrequencyCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new GZipDictionaryCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new GZipCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new PureArithCodingCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new PureHuffmanCompressionModel());
        testModel2(PreambleString, PreambleDictionary, new NoopCompressionModel());
    }
    
    private static String dictionaryToString(byte[] dictionary) {
        int i = 0, count;
        for (i = 0, count = dictionary.length; i < count && dictionary[i] == 0; i++) {
        }
        return new String(Arrays.copyOfRange(dictionary, i, dictionary.length));
    }
    
    private void testModel2(String source, String dictionary, CompressionModel model) throws IOException {
        byte[] sourceBytes = source.getBytes();
        byte[] dictionaryBytes = dictionary == null ? null : dictionary.getBytes();
        
        model.setDictionary(dictionaryBytes);
        model.build(new ArrayDocumentList(sourceBytes));
        
        byte[] compressedBytes = model.compress(sourceBytes);
        
        System.out.println(sourceBytes.length + " compressed to " + compressedBytes.length + " using " + model.getClass().getSimpleName());

        byte[] decompressedBytes = model.decompress(compressedBytes);
        String decompressedString = new String(decompressedBytes);
        
        Assert.assertEquals(source, decompressedString);
    }

    @Test
    public void testDocumentUniquenessScoring() throws IOException {
        CompressionModel model = new OffsetNibbleFrequencyCompressionModel();
        ArrayList<byte[]> documents = new ArrayList<byte[]>();
        documents.add((new String("garrick1garrick2garrick3garrick4garrick")).getBytes("UTF-8"));
        documents.add((new String("xtoubassigarrick")).getBytes("UTF-8"));
        documents.add((new String("ytoubassi")).getBytes("UTF-8"));
        documents.add((new String("ztoubassi")).getBytes("UTF-8"));
        
        model.build(new ArrayDocumentList(documents));
        
        String dictionary = dictionaryToString(model.getDictionary());
        Assert.assertEquals("garricktoubassi", dictionary);
    }

    @Test
    public void testNonexistantStrings() throws IOException {
        CompressionModel model = new OffsetNibbleFrequencyCompressionModel();
        ArrayList<byte[]> documents = new ArrayList<byte[]>();
        documents.add((new String("http://espn.de")).getBytes("UTF-8"));
        documents.add((new String("http://popsugar.de")).getBytes("UTF-8"));
        documents.add((new String("http://google.de")).getBytes("UTF-8"));
        documents.add((new String("http://yahoo.de")).getBytes("UTF-8"));
        documents.add((new String("gtoubassi")).getBytes("UTF-8"));
        documents.add((new String("gtoubassi")).getBytes("UTF-8"));
        
        model.build(new ArrayDocumentList(documents));
        
        String dictionary = dictionaryToString(model.getDictionary());
        // Make sure it doesn't think .dehttp:// is a good one
        Assert.assertEquals("gtoubassihttp://", dictionary);
    }
}

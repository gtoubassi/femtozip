package org.toubassi.femtozip;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;


public class DictionaryOptimizerTest {
    
    @Test
    public void testSubstrings() throws IOException {
        
        
        DictionaryOptimizer optimizer = new DictionaryOptimizer(new ArrayDocumentList("a man a plan a canal panama"));
        optimizer.optimize(64*1024);
        
        Assert.assertEquals(2, optimizer.getSubstringCount());
        Assert.assertEquals(25, optimizer.getSubstringScore(0));
        Assert.assertEquals("n a ", new String(optimizer.getSubstringBytes(0), "UTF-8"));
        Assert.assertEquals(40, optimizer.getSubstringScore(1));
        Assert.assertEquals("an a ", new String(optimizer.getSubstringBytes(1), "UTF-8"));
    }

    
    @Test
    public void testDictPack() throws IOException {
        
        
        DictionaryOptimizer optimizer = new DictionaryOptimizer(new ArrayDocumentList("11111", "11111", "00000"));
        byte[] dictionary = optimizer.optimize(64*1024);

        int i = 0, count;
        for (i = 0, count = dictionary.length; i < count && dictionary[i] == 0; i++) {
        }
        String d = new String(Arrays.copyOfRange(dictionary, i, dictionary.length));
        
        Assert.assertEquals("000011111", d);
    }

}

package org.toubassi.femtozip;

import java.io.IOException;

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

}

package org.toubassi.femtozip.substring;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.toubassi.femtozip.substring.PrefixHash;


public class PrefixHashTest {
    
    @Test
    public void testPrefixHash() throws IOException {
        String str = "a man a clan a canal panama";
        byte[] bytes = str.getBytes("UTF-8");
        
        PrefixHash hash = new PrefixHash(bytes, false);
        for (int i = 0; i < 12; i++) {
            hash.put(i);
        }
        
        int[] matchIndex = new int[1];
        int[] matchLength = new int[1];
        
        hash.getBestMatch(12, bytes, matchIndex, matchLength);
        
        Assert.assertEquals(5, matchIndex[0]);
        Assert.assertEquals(4, matchLength[0]);
    }

    @Test
    public void testPrefixHashWithTargetBuf() throws IOException {
        String str = "a man a clan a canal panama";
        byte[] bytes = str.getBytes("UTF-8");
        
        PrefixHash hash = new PrefixHash(bytes, true);        
        int[] matchIndex = new int[1];
        int[] matchLength = new int[1];
        
        String target = "xxx a ca";
        byte[] targetBytes = target.getBytes("UTF-8");        
        hash.getBestMatch(3, targetBytes, matchIndex, matchLength);
        
        Assert.assertEquals(12, matchIndex[0]);
        Assert.assertEquals(5, matchLength[0]);
    }

    @Test
    public void testMatchMiss() throws IOException {
        String str = "a man a clan a canal panama";
        byte[] bytes = str.getBytes("UTF-8");
        
        PrefixHash hash = new PrefixHash(bytes, true);        
        int[] matchIndex = new int[1];
        int[] matchLength = new int[1];
        
        String target = "blah!";
        byte[] targetBytes = target.getBytes("UTF-8");        
        hash.getBestMatch(0, targetBytes, matchIndex, matchLength);
        
        Assert.assertEquals(0, matchIndex[0]);
        Assert.assertEquals(0, matchLength[0]);
    }

}

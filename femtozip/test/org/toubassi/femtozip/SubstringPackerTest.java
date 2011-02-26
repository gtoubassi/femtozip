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

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.models.VerboseStringCompressionModel;



public class SubstringPackerTest {
    
    @Test
    public void testInitialDictionary() throws UnsupportedEncodingException {
        Assert.assertEquals("<-7,7> toubassi", pack("garrick toubassi", "garrick"));
        Assert.assertEquals("garrick <-16,8>", pack("garrick toubassi", "toubassi"));
        Assert.assertEquals("<-7,7> <-24,8>", pack("garrick toubassi", "toubassi garrick"));
        Assert.assertEquals("<-3,7>", pack("aaaaaaa", "aaa"));
    }

    @Test
    public void testRunLengthEncoding() throws UnsupportedEncodingException {
        Assert.assertEquals("", pack(""));
        Assert.assertEquals("a", pack("a"));
        Assert.assertEquals("aa", pack("aa"));
        Assert.assertEquals("aaa", pack("aaa"));
        Assert.assertEquals("a<-1,4>", pack("aaaaa"));
        Assert.assertEquals("a <-2,8>", pack("a a a a a "));
        Assert.assertEquals("a <-2,7>", pack("a a a a a"));
        Assert.assertEquals("a <-2,7>x", pack("a a a a ax"));
    }

    @Test
    public void testNextMatchBetterThanPreviousMatch() throws UnsupportedEncodingException {
        Assert.assertEquals("arrickgarg<-10,6>", pack("arrickgargarrick"));
    }
    
    @Test
    public void testMultipleMatches() throws UnsupportedEncodingException {
        Assert.assertEquals("garrick <-8,8>nadim<-6,7>toubassi<-9,9>", pack("garrick garrick nadim nadim toubassi toubassi"));
    }
    
    @Test
    public void testSimpleRepetitions() throws UnsupportedEncodingException {
        Assert.assertEquals("garrick <-8,7>", pack("garrick garrick"));
        Assert.assertEquals("garrick <-8,15>", pack("garrick garrick garrick"));
        Assert.assertEquals("garrick <-8,15>x", pack("garrick garrick garrickx"));
        Assert.assertEquals("garrick <-8,15>xx", pack("garrick garrick garrickxx"));
        Assert.assertEquals("garrick <-8,15>xxx", pack("garrick garrick garrickxxx"));
        Assert.assertEquals("garrick toubassi <-17,24>", pack("garrick toubassi garrick toubassi garrick"));
        Assert.assertEquals("garrick toubassi <-17,17>x<-19,8>", pack("garrick toubassi garrick toubassi x garrick"));
        Assert.assertEquals("garrick toubassi <-17,8><-25,16>", pack("garrick toubassi garrick garrick toubassi"));
    }
    
    private String pack(String s) {
        return pack(s, null);
    }

    private String pack(String s, String dict) {
        try {
            byte[] bytes = s.getBytes("UTF-8");
            byte[] dictBytes = dict == null ? null : dict.getBytes("UTF-8");
            
            VerboseStringCompressionModel model = new VerboseStringCompressionModel();
            model.setDictionary(dictBytes);
            byte[] compressed = model.compress(bytes);
            byte[] decompressed = model.decompress(compressed);
            
            Assert.assertArrayEquals(bytes, decompressed);
            
            return new String(compressed, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}

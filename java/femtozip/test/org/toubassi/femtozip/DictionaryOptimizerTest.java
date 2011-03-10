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

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
        
        Match match = hash.getBestMatch(12, bytes);
        
        Assert.assertEquals(5, match.bestMatchIndex);
        Assert.assertEquals(4, match.bestMatchLength);
    }

    @Test
    public void testPrefixHashWithTargetBuf() throws IOException {
        String str = "a man a clan a canal panama";
        byte[] bytes = str.getBytes("UTF-8");
        
        PrefixHash hash = new PrefixHash(bytes, true);        
        
        String target = "xxx a ca";
        byte[] targetBytes = target.getBytes("UTF-8");        
        Match match = hash.getBestMatch(3, targetBytes);
        
        Assert.assertEquals(12, match.bestMatchIndex);
        Assert.assertEquals(5, match.bestMatchLength);
    }

    @Test
    public void testMatchMiss() throws IOException {
        String str = "a man a clan a canal panama";
        byte[] bytes = str.getBytes("UTF-8");
        
        PrefixHash hash = new PrefixHash(bytes, true);        
        
        String target = "blah!";
        byte[] targetBytes = target.getBytes("UTF-8");        
        Match match = hash.getBestMatch(0, targetBytes);
        
        Assert.assertEquals(0, match.bestMatchIndex);
        Assert.assertEquals(0, match.bestMatchLength);
    }

}

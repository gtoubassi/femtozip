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
package org.toubassi.femtozip.coding.arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.colloquial.arithcode.ppm.ArithCodeModel;


public class FrequencyCodeModelTest {
    
    @Test
    public void testIntegerSymbols() throws IOException {
        String preamble = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
        short[] data = new short[preamble.length() + 100];
        Random random = new Random(12345);

        int i, count;
        for (i = 0, count = preamble.length(); i < count; i++) {
            data[i] = (short)preamble.charAt(i);
        }
        for (count = data.length; i < count; i++) {
            data[i] = (short)(256 + 1 + random.nextInt(25));
        }
        data[data.length - 1] = 1;
        
        ArithCodeModel model = new FrequencyCodeModel(data, 281, true);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ArithCodeWriter arithWriter = new ArithCodeWriter(bytesOut, model);
        for (i = 0, count = data.length; i < count; i++) {
            arithWriter.writeSymbol(data[i]);
        }
        arithWriter.close();
        
        byte[] compressedBytes = bytesOut.toByteArray();
        
        System.out.println(data.length + " compressed to " + compressedBytes.length);
        
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedBytes);
        ArithCodeReader arithReader = new ArithCodeReader(bytesIn, model);
        int nextByte;
        short[] readData = new short[data.length];
        int index = 0;
        while ((nextByte = arithReader.readSymbol()) >= 0) {
            readData[index++] = (short)nextByte;
        }
        arithReader.close();
        Assert.assertTrue(Arrays.equals(data, readData));
    }

    @Test
    public void testModel() throws IOException {
        String preamble = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
        verify(preamble, preamble.length(), 176);
        
        preamble = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
        verify(preamble, preamble.length() - 1, 207);
        
        preamble = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
        verify(preamble, 100, 244);
        
        preamble = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
        verify(preamble, 25, 296);
    }
    
    private void verify(String source, int modelSymbolSampleSize, int expectedCompressedSize) {
        try {
            byte[] sourceBytes = source.getBytes("UTF-8");
            modelSymbolSampleSize = Math.min(sourceBytes.length, modelSymbolSampleSize);
            byte[] symbolSampleBytes;
            if (modelSymbolSampleSize < sourceBytes.length) {
                symbolSampleBytes = Arrays.copyOf(sourceBytes, modelSymbolSampleSize);
            }
            else {
                symbolSampleBytes = sourceBytes;
            }
            ArithCodeModel model = new FrequencyCodeModel(symbolSampleBytes, modelSymbolSampleSize < sourceBytes.length ? false : true);
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ArithCodeWriter arithWriter = new ArithCodeWriter(bytesOut, model);
            for (int i = 0, count = sourceBytes.length; i < count; i++) {
                arithWriter.writeSymbol(((int)sourceBytes[i]) & 0xff);
            }
            arithWriter.close();

            byte[] compressedSourceBytes = bytesOut.toByteArray();
            System.out.println(sourceBytes.length + " compressed to " + compressedSourceBytes.length);
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressedSourceBytes);
            ArithCodeReader arithReader = new ArithCodeReader(bytesIn, model);
            bytesOut.reset();
            int nextByte;
            while ((nextByte = arithReader.readSymbol()) >= 0) {
                bytesOut.write(nextByte);
            }
            arithReader.close();
            
            Assert.assertEquals(source, new String(bytesOut.toByteArray(), "UTF-8"));
            Assert.assertEquals(expectedCompressedSize, compressedSourceBytes.length);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package org.toubassi.femtozip.models;

import java.io.IOException;

import org.junit.Test;
import org.toubassi.femtozip.CompressionTest;


public class NativeCompressionModelTest {
    
    @Test
    public void testNativeModel() throws IOException {
        NativeCompressionModel model = new NativeCompressionModel();

        CompressionTest.testModel(CompressionTest.PreambleString, CompressionTest.PreambleDictionary, model, -1);

    }

}

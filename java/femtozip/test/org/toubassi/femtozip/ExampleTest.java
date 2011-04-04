package org.toubassi.femtozip;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;


public class ExampleTest {

    @Test
    public void example() throws IOException {
        ArrayDocumentList trainingDocs = new ArrayDocumentList("http://espn.de", "http://popsugar.de",
                "http://google.de", "http://yahoo.de", "http://www.linkedin.com", "http://www.facebook.com",
                "http:www.stanford.edu");
        
        CompressionModel model = CompressionModel.buildOptimalModel(trainingDocs);
        byte[] data = "check out http://www.facebook.com/someone".getBytes("UTF-8");
        byte[] compressed = model.compress(data);
        
        byte[] decompressed = model.decompress(compressed);
        
        Assert.assertArrayEquals(data, decompressed);
    }
}

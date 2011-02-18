package org.toubassi.femtozip.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtil {

    public static int readBytes(InputStream in, byte[] buf, int length) throws IOException {
        int totalRead = 0;
        int numRead = 0;
        while ((numRead = in.read(buf, totalRead, length - totalRead)) > -1 && totalRead < length) {
            totalRead += numRead;
        }
        return totalRead;
    }

    public static int readAll(InputStream in, byte[] buf) throws IOException {
        return readBytes(in, buf, buf.length);
    }

    public static byte[] readAll(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int numRead = 0;
        while ((numRead = in.read(buf)) > -1) {
            bytesOut.write(buf, 0, numRead);
        }
        return bytesOut.toByteArray();
    }
}

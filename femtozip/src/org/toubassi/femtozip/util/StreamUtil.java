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

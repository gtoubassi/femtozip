package org.toubassi.femtozip;

import java.io.IOException;

public interface DocumentList {
    public int size();
    public byte[] get(int i) throws IOException;
}

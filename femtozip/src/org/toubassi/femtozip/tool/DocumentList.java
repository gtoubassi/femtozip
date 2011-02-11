package org.toubassi.femtozip.tool;

import java.io.IOException;

public interface DocumentList {
    public int size();
    public String getName(int i);
    public byte[] get(int i) throws IOException;
    public void setCompressed(int i, byte[] data) throws IOException;
    public void setDecompressed(int i, byte[] data) throws IOException;
}

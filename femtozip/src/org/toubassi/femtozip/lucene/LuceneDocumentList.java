package org.toubassi.femtozip.lucene;

import java.io.IOException;

import org.toubassi.femtozip.tool.DocumentList;

public class LuceneDocumentList implements DocumentList {

    public int size() {
        return 0;
    }

    public String getName(int i) {
        return null;
    }

    public byte[] get(int i) throws IOException {
        return null;
    }

    public void setCompressed(int i, byte[] data) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void setDecompressed(int i, byte[] data) throws IOException {
        throw new UnsupportedOperationException();
    }
}

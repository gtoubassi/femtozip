package org.toubassi.femtozip;

import java.util.ArrayList;

public class ArrayDocumentList implements DocumentList {
    
    private ArrayList<byte[]> docs;
    
    public ArrayDocumentList(ArrayList<byte[]> documents) {
        this.docs = documents;
    }

    public ArrayDocumentList(byte[] singleDocument) {
        docs = new ArrayList<byte[]>(1);
        docs.add(singleDocument);
    }

    public int size() {
        return docs.size();
    }

    public byte[] get(int i) {
        return docs.get(i);
    }
}

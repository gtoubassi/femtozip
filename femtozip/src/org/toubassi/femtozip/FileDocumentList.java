package org.toubassi.femtozip;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.toubassi.femtozip.util.FileUtil;


public class FileDocumentList implements DocumentList {
    private String basePath;
    private List<String> files;
    
    public FileDocumentList(List<String> files) {
        this(null, files);
    }

    public FileDocumentList(String basePath, List<String> files) {
        this.basePath = basePath;
        this.files = files;
    }

    public int size() {
        return files.size();
    }
    
    public byte[] get(int i) throws IOException {
        String path = basePath == null ? files.get(i) : (basePath + File.separator + files.get(i));
        return FileUtil.readFile(path);
    }
}

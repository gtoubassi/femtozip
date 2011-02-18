package org.toubassi.femtozip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class FileDocumentList implements DocumentList {
    private ArrayList<String> files;
    
    public FileDocumentList(ArrayList<String> files) {
        this.files = files;
    }

    public int size() {
        return files.size();
    }
    
    public byte[] get(int i) throws IOException {
        return readFile(files.get(i));
    }
    
    public String getName(int i) {
        return files.get(i);
    }

    public void setCompressed(int i, byte[] data) throws IOException {
        writeFile(files.get(i) + ".fzip", data);
    }
    
    public void setDecompressed(int i, byte[] data) throws IOException {
        String file = files.get(i);
        if (file.endsWith(".fzip")) {
            file = file.substring(0, file.length() - 5);
        }
        writeFile(file + ".unfzip", data);
    }

    private byte[] readFile(String file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            int fileLength = (int)(new File(file)).length();
            byte[] fileBytes = new byte[fileLength];
            int totalRead = 0;
            int numRead = 0;
            while ((numRead = in.read(fileBytes, totalRead, fileLength - totalRead)) > -1 && totalRead < fileLength) {
                totalRead += numRead;
            }
            return fileBytes;
        }
        finally {
            in.close();
        }
    }
    
    private void writeFile(String file, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(data);
        }
        finally {
            out.close();
        }
    }
    
}

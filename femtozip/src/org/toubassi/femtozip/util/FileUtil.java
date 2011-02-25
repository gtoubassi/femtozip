package org.toubassi.femtozip.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {
    
    public static long computeSize(File root) {
        File[] files = root.listFiles();
        
        long size = 0;
        for (File subFile : files) {
            if (!subFile.getPath().endsWith(".fzmodel")) {
                size += subFile.length();
                if (subFile.isDirectory()) {
                    size += computeSize(subFile);
                }
            }
        }
        
        return size;
    }
    
    
    public static boolean recursiveDelete(File file) {
        boolean status = true;
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (!f.getName().equals(".") && !f.getName().equals("..")) {
                    status = status && recursiveDelete(f);
                }
            }
        }
        return status && file.delete();
    }
    
    public static byte[] readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        // No need to buffer as StreamUtil.readAll will read in big chunks
        return StreamUtil.readAll(in);
    }

    public static byte[] readFile(String path) throws IOException {
        FileInputStream in = new FileInputStream(path);
        // No need to buffer as StreamUtil.readAll will read in big chunks
        return StreamUtil.readAll(in);
    }

}

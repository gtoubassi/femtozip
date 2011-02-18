package org.toubassi.femtozip.util;

import java.io.File;

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

}

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

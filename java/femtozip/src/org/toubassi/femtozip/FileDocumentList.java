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
package org.toubassi.femtozip;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.toubassi.femtozip.util.FileUtil;


public class FileDocumentList implements DocumentList {
    private String basePath;
    private List<String> files;
    private List<byte[]> data;
    
    public FileDocumentList(List<String> files) throws IOException {
        this(null, files, false);
    }

    public FileDocumentList(String basePath, List<String> files) throws IOException {
        this(basePath, files, false);
    }
    
    public FileDocumentList(String basePath, List<String> files, boolean preload) throws IOException {
        this.basePath = basePath;
        this.files = files;
        if (preload) {
            data = new ArrayList<byte[]>(files.size());
            for (int i = 0, count = files.size(); i < count; i++) {
                data.add(loadFile(i));
            }
        }
    }

    public int size() {
        return files.size();
    }
    
    public byte[] get(int i) throws IOException {
        return data != null ? data.get(i) : loadFile(i);
    }
    
    private byte[] loadFile(int i) throws IOException {
        String path = basePath == null ? files.get(i) : (basePath + File.separator + files.get(i));
        return FileUtil.readFile(path);
    }
}

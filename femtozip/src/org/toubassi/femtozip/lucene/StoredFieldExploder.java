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
package org.toubassi.femtozip.lucene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;

public class StoredFieldExploder {
    
    private String indexPath;
    private String fieldName;
    private int numSamples;
    private String outputBasePath;

    protected void usage() {
        System.out.println("usage: --field fieldname --numsamples number --outputbasepath dir indexpath");
        System.exit(1);
    }
    
    protected void parseArguments(String[] args) {
        
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.equals("--outputbasepath")) {
                outputBasePath = args[++i];
            }
            if (arg.equals("--numsamples")) {
                numSamples = Integer.parseInt(args[++i]);
            }
            else if (arg.equals("--field")) {
                fieldName = args[++i];
            }
            else {
                indexPath = arg;
            }
        }

        if (indexPath == null || fieldName == null || numSamples <= 0 || outputBasePath == null) {
            usage();
        }
    }
    
    protected void dump() throws IOException {
        IndexReader reader = IndexReader.open(indexPath);

        Collection allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);
        
        int numProcessed = 0;
        
        for (int docId = 0, count = reader.maxDoc(); docId < count && numProcessed < numSamples; docId++) {
            if (reader.isDeleted(docId)) {
                continue;
            }

            Document doc = reader.document(docId);
            Field field = doc.getField(fieldName);
            
            
            if (field != null) {
                
                FileOutputStream out = new FileOutputStream(outputBasePath + File.separator + (numProcessed + 1) + "." + fieldName);
                if (field.isBinary()) {
                    out.write(field.getBinaryValue(), field.getBinaryOffset(), field.getBinaryLength());
                }
                else {
                    out.write(field.stringValue().getBytes("UTF-8"));
                }
                out.close();
                
                numProcessed++;
            }
        }
        
        reader.close();
    }
    
    
    protected void run(String[] args) throws IOException {
        parseArguments(args);
        
        dump();
    }
    
    public static void main(String[] args) throws IOException {
        StoredFieldExploder exploder = new StoredFieldExploder();
        exploder.run(args);
    }
}

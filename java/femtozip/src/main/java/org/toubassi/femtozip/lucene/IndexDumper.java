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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;

public class IndexDumper  {
    
    private String indexPath;
    
    private int numSamples;
    private int numDocs;
    private List<String> fieldsToDump;
    
    protected void dump() throws IOException {
        IndexReader reader = IndexReader.open(indexPath);
        
        Collection<?> allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);
        
        numDocs = reader.numDocs();
        int maxDocId = reader.maxDoc();
        float samplingRate = ((float)numSamples) / numDocs;

        int numDocsScanned = 0;
        int numDocsSampled = 0;
        for (int docId = 0; docId < maxDocId; docId++) {
            
            if (reader.isDeleted(docId)) {
                continue;
            }
            
            numDocsScanned++;
            
            if (((int)(numDocsScanned * samplingRate)) <= numDocsSampled) {
                continue;
            }
            
            numDocsSampled++;

            Document doc = reader.document(docId);
            
            System.out.println("DOCUMENT: " + docId);
            
            for (String fieldName : fieldNames) {
                if (fieldsToDump != null && fieldsToDump.indexOf(fieldName) == -1) {
                    continue;
                }
                
                Field[] fields = doc.getFields(fieldName);
                
                for (Field field : fields) {
                    
                    if (!field.isStored() || field.isCompressed()) {
                        // TODO if its compressed, uncompress it and benchmark it.
                        continue;
                    }
                    
                    byte[] bytes;
                    
                    if (field.isBinary()) {
                        bytes = new byte[field.getBinaryLength()];
                        System.arraycopy(field.getBinaryValue(), field.getBinaryOffset(), bytes, 0, field.getBinaryLength());
                    }
                    else {
                        String value = field.stringValue();
                        bytes = value.getBytes("UTF-8");
                    }
                    
                    if (bytes.length > 0) {
                        System.out.print("    " + fieldName + " " + bytes.length + " ");
                        System.out.write(bytes);
                        System.out.println();
                    }
                }
            }
        }
        
        reader.close();
    }
    
    protected static void usage() {
        System.out.println("Usage: --numsamples number [--fields field1,field2] indexpath");
        System.exit(1);
    }
    
    public void run(String[] args) throws IOException {
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.equals("--numsamples")) {
                numSamples = Integer.parseInt(args[++i]);
            }
            else if (arg.equals("--fields")) {
                fieldsToDump = Arrays.asList(args[++i].split(","));
            }
            else {
                indexPath = arg;
            }
        }
        
        if (indexPath == null) {
            usage();
        }
        
        dump();
    }
    
    public static void main(String[] args) throws IOException {
        IndexDumper dumper = new IndexDumper();
        dumper.run(args);
    }
    
}

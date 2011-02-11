package org.toubassi.femtozip.lucene;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;

public class StoredFieldDumper {
    
    private String indexPath;
    private String outputBasePath;

    protected void parseArguments(String[] args) {
        
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.equals("--outputbasepath")) {
                outputBasePath = args[++i];
            }
            else {
                indexPath = arg;
            }
        }
    }
    
    protected void dump() throws IOException {
        IndexReader reader = IndexReader.open(indexPath);

        Collection allFields = reader.getFieldNames(IndexReader.FieldOption.ALL);
        String[] fieldNames = new String[allFields.size()];
        allFields.toArray(fieldNames);
        
        Map<String, OutputStream> output = new HashMap<String, OutputStream>();
        
        long lastStatusTime = 0;
        
        for (int docId = 0, count = reader.maxDoc(); docId < count; docId++) {
            Document doc = reader.document(docId);
            
            if (System.currentTimeMillis() - lastStatusTime > 5000) {
                lastStatusTime = System.currentTimeMillis();
                System.out.println("Processing docId " + docId + " of " + count);
            }
            
            for (String fieldName : fieldNames) {
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
                    
                    OutputStream out = output.get(fieldName);
                    if (out == null) {
                        FileOutputStream fileOut = new FileOutputStream(outputBasePath + "_" + fieldName);
                        out = new BufferedOutputStream(fileOut);
                        output.put(fieldName, out);
                    }
                    
                    out.write(bytes);
                }
            }
        }
        
        reader.close();
        
        for (Map.Entry<String, OutputStream> entry : output.entrySet()) {
            entry.getValue().close();
        }
    }
    
    
    protected void run(String[] args) throws IOException {
        parseArguments(args);
        
        dump();
    }
    
    public static void main(String[] args) throws IOException {
        StoredFieldDumper dumper = new StoredFieldDumper();
        dumper.run(args);
    }
}

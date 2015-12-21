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

import java.io.IOException;

public class SamplingDocumentList implements DocumentList {
    
    private DocumentList documents;
    private int numPartitions;
    private int partition;
    private int size;
    
    public SamplingDocumentList(DocumentList documents, int numPartitions, int partition) {
        this.documents = documents;
        if (numPartitions < 1) {
            throw new IllegalArgumentException("numPartitions must be >= 1");
        }
        if (partition < 0 || partition >= numPartitions) {
            throw new IllegalArgumentException("partition must be in the range of [0,numPartitions)");
        }
        this.numPartitions = numPartitions;
        this.partition = partition;
        int docsSize = documents.size();
        this.size = Math.max(1, docsSize / numPartitions + ((docsSize / numPartitions) > partition ? 1 : 0));
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public byte[] get(int i) throws IOException {
        i = Math.min(documents.size() - 1, i * numPartitions + partition);
        return documents.get(i);
    }

}

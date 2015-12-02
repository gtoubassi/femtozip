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
package org.toubassi.femtozip.coding.huffman;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FrequencyHuffmanModel implements HuffmanModel {
    
    private Codeword[] encoding;
    private DecodeTable decoding;
    
    public static int[] computeHistogramWithEOFSymbol(byte[] data) {
        int[] histogram = new int[256 + 1];
        for (int i = 0, count = data.length; i < count; i++) {
            histogram[((int)data[i]) & 0xff]++;
        }
        histogram[histogram.length - 1] = 1; // EOF
        return histogram;
    }
    
    public FrequencyHuffmanModel(int[] histogram, boolean allSymbolsSampled) {
        
        if (!allSymbolsSampled) {
            for (int i = 0, count = histogram.length; i < count; i++) {
                if (histogram[i] == 0) {
                    histogram[i] = 1;
                }
            }
        }
        
        computeHuffmanCoding(histogram);
    }
    
    public FrequencyHuffmanModel(DataInputStream in) throws IOException {
        load(in);
    }

    public void load(DataInputStream in) throws IOException {
        encoding = new Codeword[in.readInt()];
        for (int i = 0, count = encoding.length; i < count; i++) {
            if (in.readBoolean()) {
                encoding[i] = new Codeword();
                encoding[i].load(in);
            }
        }
        decoding = new DecodeTable();
        decoding.build(encoding);
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(encoding.length);
        for (int i = 0, count = encoding.length; i < count; i++) {
            if (encoding[i] != null) {
                out.writeBoolean(true);
                encoding[i].save(out);
            }
            else {
                out.writeBoolean(false);
            }
        }
    }
    
    protected void computeHuffmanCoding(int[] histogram) {
        List<HuffmanNode> queue1 = new ArrayList<HuffmanNode>();
        List<HuffmanNode> queue2 = new ArrayList<HuffmanNode>();

        for (int i = 0, count = histogram.length; i < count; i++) {
            if (histogram[i] != 0) {
                queue1.add(new HuffmanNode(null, null, histogram[i], i));
            }
        }
        
        Collections.sort(queue1, new HuffmanNodeWeightComparator());
        
        while (queue1.size() + queue2.size() > 1) {
            List<HuffmanNode> candidateQueue1 = null;
            List<HuffmanNode> candidateQueue2 = null;
            int candidateWeight = Integer.MAX_VALUE;
            
            if (queue1.size() > 0 && queue2.size() > 0) {
                if (queue1.get(0).weight + queue2.get(0).weight < candidateWeight) {
                    candidateWeight = queue1.get(0).weight + queue2.get(0).weight;
                    candidateQueue1 = queue1;
                    candidateQueue2 = queue2;
                }
            }
            if (queue1.size() > 1) {
                if (queue1.get(0).weight + queue1.get(1).weight < candidateWeight) {
                    candidateWeight = queue1.get(0).weight + queue1.get(1).weight;
                    candidateQueue1 = candidateQueue2 = queue1;
                }
            }
            if (queue2.size() > 1) {
                if (queue2.get(0).weight + queue2.get(1).weight < candidateWeight) {
                    candidateWeight = queue2.get(0).weight + queue2.get(1).weight;
                    candidateQueue1 = candidateQueue2 = queue2;
                }
            }
            
            HuffmanNode left = candidateQueue1.remove(0);
            HuffmanNode right = candidateQueue2.remove(0);
            HuffmanNode newNode = new HuffmanNode(left, right, candidateWeight, null);
            queue2.add(newNode);
        }
        
        encoding = new Codeword[histogram.length];
        queue2.get(0).collectPrefixes(encoding, new Codeword());
        decoding = new DecodeTable();
        decoding.build(encoding);
    }
    
    public Codeword getCodewordForEOF() {
        return encoding[encoding.length - 1];
    }
    
    public Codeword encode(int symbol) {
        return encoding[symbol];
    }
    
    public Codeword decode(int bits) {
        return decoding.decode(bits);
    }
    
    public boolean isEOF(Codeword codeword) {
        return codeword.equals(getCodewordForEOF());
    }
}

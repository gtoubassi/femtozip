package org.toubassi.femtozip.coding.huffman;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HuffmanModel {
    
    Codeword[] encoding;
    DecodeTable decoding;
    
    public HuffmanModel(int[] histogram, boolean allSymbolsSampled) {
        
        if (!allSymbolsSampled) {
            for (int i = 0, count = histogram.length; i < count; i++) {
                if (histogram[i] == 0) {
                    histogram[i] = 1;
                }
            }
        }
        
        histogram[histogram.length - 1] = 1;
        computeHuffmanCoding(histogram);
    }
    
    public HuffmanModel(DataInputStream in) throws IOException {
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
        decoding.load(in);
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
        decoding.save(out);
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
}

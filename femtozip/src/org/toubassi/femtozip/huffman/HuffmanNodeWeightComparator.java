package org.toubassi.femtozip.huffman;

import java.util.Comparator;

public class HuffmanNodeWeightComparator implements Comparator<HuffmanNode> {

    public int compare(HuffmanNode o1, HuffmanNode o2) {
        return o1.weight - o2.weight;
    }

}

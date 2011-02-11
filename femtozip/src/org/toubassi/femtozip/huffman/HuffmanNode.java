package org.toubassi.femtozip.huffman;


public class HuffmanNode {

    public HuffmanNode left;
    public HuffmanNode right;
    public int weight;
    public Integer symbol;
    
    public HuffmanNode(HuffmanNode left, HuffmanNode right, int weight, Integer symbol) {
        this.left = left;
        this.right = right;
        this.weight = weight;
        this.symbol = symbol;
    }

    void collectPrefixes(Codeword prefixes[], Codeword current) {
        if (symbol == null) {
            Codeword c = current.clone();
            c.appendBit(0);
            left.collectPrefixes(prefixes, c);
            c = current.clone();
            c.appendBit(1);
            right.collectPrefixes(prefixes, c);
        }
        else {
            prefixes[symbol] = current;
            current.symbol = symbol;
        }
    }
}

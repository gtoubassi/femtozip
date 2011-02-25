package org.toubassi.femtozip.coding.huffman;

public interface HuffmanModel {
    public Codeword getCodewordForEOF();
    public Codeword encode(int symbol);
    public Codeword decode(int bits);
    public boolean isEOF(Codeword codeword);
}

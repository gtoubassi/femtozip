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

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
package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.toubassi.femtozip.coding.huffman.Codeword;
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
import org.toubassi.femtozip.coding.huffman.HuffmanModel;

public class FemtoZipHuffmanModel implements HuffmanModel, Cloneable {

    private enum State {
        LiteralLengthState, OffsetNibble0State, OffsetNibble1State, OffsetNibble2State, OffsetNibble3State;
    }

    private FrequencyHuffmanModel literalLengthModel;
    private FrequencyHuffmanModel offsetNibble0Model;
    private FrequencyHuffmanModel offsetNibble1Model;
    private FrequencyHuffmanModel offsetNibble2Model;
    private FrequencyHuffmanModel offsetNibble3Model;
    private State state = State.LiteralLengthState;

    public FemtoZipHuffmanModel(FrequencyHuffmanModel literalLengthModel,
            FrequencyHuffmanModel offsetNibble0Model,
            FrequencyHuffmanModel offsetNibble1Model,
            FrequencyHuffmanModel offsetNibble2Model,
            FrequencyHuffmanModel offsetNibble3Model)
    {
        this.literalLengthModel = literalLengthModel;
        this.offsetNibble0Model = offsetNibble0Model;
        this.offsetNibble1Model = offsetNibble1Model;
        this.offsetNibble2Model = offsetNibble2Model;
        this.offsetNibble3Model = offsetNibble3Model;
    }
    
    public FemtoZipHuffmanModel(DataInputStream in) throws IOException {
        literalLengthModel = new FrequencyHuffmanModel(in);
        offsetNibble0Model = new FrequencyHuffmanModel(in);
        offsetNibble1Model = new FrequencyHuffmanModel(in);
        offsetNibble2Model = new FrequencyHuffmanModel(in);
        offsetNibble3Model = new FrequencyHuffmanModel(in);
    }
    
    public FemtoZipHuffmanModel createModel() {
        try {
            return (FemtoZipHuffmanModel)clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void save(DataOutputStream out) throws IOException {
        literalLengthModel.save(out);
        offsetNibble0Model.save(out);
        offsetNibble1Model.save(out);
        offsetNibble2Model.save(out);
        offsetNibble3Model.save(out);
    }

    public Codeword getCodewordForEOF() {
        return literalLengthModel.getCodewordForEOF();
    }

    public Codeword encode(int symbol) {
        switch (state) {
        case LiteralLengthState:
            if (symbol > 255) {
                state = State.OffsetNibble0State;
            }
            return literalLengthModel.encode(symbol);
        case OffsetNibble0State:
            state = State.OffsetNibble1State;
            return offsetNibble0Model.encode(symbol);
        case OffsetNibble1State:
            state = State.OffsetNibble2State;
            return offsetNibble1Model.encode(symbol);
        case OffsetNibble2State:
            state = State.OffsetNibble3State;
            return offsetNibble2Model.encode(symbol);
        case OffsetNibble3State:
            state = State.LiteralLengthState;
            return offsetNibble3Model.encode(symbol);
        default:
            throw new RuntimeException();
        }
    }

    public Codeword decode(int bits) {
        switch (state) {
        case LiteralLengthState:
            Codeword codeword = literalLengthModel.decode(bits);
            if (codeword.getSymbol() > 255) {
                state = State.OffsetNibble0State;
            }
            return codeword;
        case OffsetNibble0State:
            state = State.OffsetNibble1State;
            return offsetNibble0Model.decode(bits);
        case OffsetNibble1State:
            state = State.OffsetNibble2State;
            return offsetNibble1Model.decode(bits);
        case OffsetNibble2State:
            state = State.OffsetNibble3State;
            return offsetNibble2Model.decode(bits);
        case OffsetNibble3State:
            state = State.LiteralLengthState;
            return offsetNibble3Model.decode(bits);
        default:
            throw new RuntimeException();
        }
    }
    
    public boolean isEOF(Codeword codeword) {
        return state == State.OffsetNibble0State && getCodewordForEOF().equals(codeword);
    }
}

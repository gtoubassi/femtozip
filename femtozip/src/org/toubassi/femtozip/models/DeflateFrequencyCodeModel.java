package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;

import com.colloquial.arithcode.ppm.ArithCodeModel;

public class DeflateFrequencyCodeModel implements ArithCodeModel {
    private enum State {
        LiteralLengthState, OffsetState;
    }
    
    private FrequencyCodeModel literalLengthModel;
    private FrequencyCodeModel offsetModel;
    private State state = State.LiteralLengthState;
    private State nextState;
    
    
    public DeflateFrequencyCodeModel(FrequencyCodeModel literalLengthModel, FrequencyCodeModel offsetModel) {
        this.literalLengthModel = literalLengthModel;
        this.offsetModel = offsetModel;
    }
    
    public DeflateFrequencyCodeModel(DataInputStream in) throws IOException {
        literalLengthModel = new FrequencyCodeModel(in);
        offsetModel = new FrequencyCodeModel(in);
    }
    
    public void save(DataOutputStream out) throws IOException {
        literalLengthModel.save(out);
        offsetModel.save(out);
    }

    public int totalCount() {
        switch (state) {
        case LiteralLengthState:
            return literalLengthModel.totalCount();
        case OffsetState:
            return offsetModel.totalCount();
        default:
            throw new RuntimeException();
        }
    }

    public int pointToSymbol(int count) {
        //XXX HACK
        if (nextState != null) {
            state = nextState;
            nextState = null;
        }
        
        switch (state) {
        case LiteralLengthState:
            int symbol = literalLengthModel.pointToSymbol(count);
            if (symbol > 255) {
                nextState = State.OffsetState;
            }
            return symbol;
        case OffsetState:
            nextState = State.LiteralLengthState;
            return offsetModel.pointToSymbol(count);
        default:
            throw new RuntimeException();
        }
    }

    public void interval(int symbol, int[] result) {
        switch (state) {
        case LiteralLengthState:
            literalLengthModel.interval(symbol, result);
            if (symbol > 255) {
                state = State.OffsetState;
            }
            break;
        case OffsetState:
            state = State.LiteralLengthState;
            offsetModel.interval(symbol, result);
            break;
        default:
            throw new RuntimeException();
        }
    }

    public boolean escaped(int symbol) {
        return false;
    }

    public void exclude(int symbol) {
    }

    public void increment(int symbol) {
    }
}

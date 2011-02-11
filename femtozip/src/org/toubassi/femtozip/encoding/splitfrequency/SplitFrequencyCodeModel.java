package org.toubassi.femtozip.encoding.splitfrequency;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.toubassi.femtozip.encoding.arithcoding.FrequencyCodeModel;

import com.colloquial.arithcode.ArithCodeModel;

public class SplitFrequencyCodeModel implements ArithCodeModel {
    static final int SUBSTRING_SYMBOL = 256;

    private enum State {
        LiteralState, LengthState, OffsetByte0State, OffsetByte1State
    }
    
    private FrequencyCodeModel literalModel;
    private FrequencyCodeModel substringModel;
    private State state = State.LiteralState;
    private State nextState = null;
    
    public SplitFrequencyCodeModel(FrequencyCodeModel literalModel, FrequencyCodeModel substringModel) {
        this.literalModel = literalModel;
        this.substringModel = substringModel;
    }
    
    public SplitFrequencyCodeModel(DataInputStream in) throws IOException {
        literalModel = new FrequencyCodeModel(in);
        substringModel = new FrequencyCodeModel(in);
    }
    
    public void save(DataOutputStream out) throws IOException {
        literalModel.save(out);
        substringModel.save(out);
    }

    @Override
    public int totalCount() {
        return state == State.LiteralState ? literalModel.totalCount() : substringModel.totalCount();
    }

    @Override
    public int pointToSymbol(int count) {
        //XXX HACK
        if (nextState != null) {
            state = nextState;
            nextState = null;
        }
        
        if (state == State.LiteralState) {
            int symbol = literalModel.pointToSymbol(count);
            if (symbol == SUBSTRING_SYMBOL) {
                nextState = State.LengthState;
            }
            return symbol;
        }
        else if (state == State.LengthState) {
            nextState = State.OffsetByte0State;
            return substringModel.pointToSymbol(count);
        }
        else if (state == State.OffsetByte0State) {
            nextState = State.OffsetByte1State;
            return substringModel.pointToSymbol(count);
        }
        else if (state == State.OffsetByte1State) {
            nextState = State.LiteralState;
            return substringModel.pointToSymbol(count);
        }
        else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void interval(int symbol, int[] result) {
        if (state == State.LiteralState) {
            literalModel.interval(symbol, result);
            if (symbol == SUBSTRING_SYMBOL) {
                state = State.LengthState;
            }
        }
        else if (state == State.LengthState) {
            state = State.OffsetByte0State;
            substringModel.interval(symbol, result);
        }
        else if (state == State.OffsetByte0State) {
            state = State.OffsetByte1State;
            substringModel.interval(symbol, result);
        }
        else if (state == State.OffsetByte1State) {
            state = State.LiteralState;
            substringModel.interval(symbol, result);
        }
        else {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean escaped(int symbol) {
        return false;
    }

    @Override
    public void exclude(int symbol) {
    }

    @Override
    public void increment(int symbol) {
    }
}

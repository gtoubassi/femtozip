package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.toubassi.femtozip.coding.arithmetic.FrequencyCodeModel;

import com.colloquial.arithcode.ArithCodeModel;

public class NibbleFrequencyCodeModel implements ArithCodeModel {
    static final int SUBSTRING_SYMBOL = 256;
    
    private enum State {
        LiteralState, LengthNibble0State, LengthNibble1State, OffsetNibble0State, OffsetNibble1State, OffsetNibble2State, OffsetNibble3State;
    }
    
    private FrequencyCodeModel literalModel;
    private FrequencyCodeModel lengthNibble0Model;
    private FrequencyCodeModel lengthNibble1Model;
    private FrequencyCodeModel offsetNibble0Model;
    private FrequencyCodeModel offsetNibble1Model;
    private FrequencyCodeModel offsetNibble2Model;
    private FrequencyCodeModel offsetNibble3Model;
    private State state = State.LiteralState;
    private State nextState = null;

    public NibbleFrequencyCodeModel(FrequencyCodeModel literalModel,
            FrequencyCodeModel lengthNibble0Model,
            FrequencyCodeModel lengthNibble1Model,
            FrequencyCodeModel offsetNibble0Model,
            FrequencyCodeModel offsetNibble1Model,
            FrequencyCodeModel offsetNibble2Model,
            FrequencyCodeModel offsetNibble3Model) {
        this.literalModel = literalModel;
        this.lengthNibble0Model = lengthNibble0Model;
        this.lengthNibble1Model = lengthNibble1Model;
        this.offsetNibble0Model = offsetNibble0Model;
        this.offsetNibble1Model = offsetNibble1Model;
        this.offsetNibble2Model = offsetNibble2Model;
        this.offsetNibble3Model = offsetNibble3Model;
    }

    public NibbleFrequencyCodeModel(DataInputStream in) throws IOException {
        literalModel = new FrequencyCodeModel(in);
        lengthNibble0Model = new FrequencyCodeModel(in);
        lengthNibble1Model = new FrequencyCodeModel(in);
        offsetNibble0Model = new FrequencyCodeModel(in);
        offsetNibble1Model = new FrequencyCodeModel(in);
        offsetNibble2Model = new FrequencyCodeModel(in);
        offsetNibble3Model = new FrequencyCodeModel(in);
    }
    
    public void save(DataOutputStream out) throws IOException {
        literalModel.save(out);
        lengthNibble0Model.save(out);
        lengthNibble1Model.save(out);
        offsetNibble0Model.save(out);
        offsetNibble1Model.save(out);
        offsetNibble2Model.save(out);
        offsetNibble3Model.save(out);
    }
    
    
    @Override
    public int totalCount() {
        switch (state) {
        case LiteralState:
            return literalModel.totalCount();
        case LengthNibble0State:
            return lengthNibble0Model.totalCount();
        case LengthNibble1State:
            return lengthNibble1Model.totalCount();
        case OffsetNibble0State:
            return offsetNibble0Model.totalCount();
        case OffsetNibble1State:
            return offsetNibble1Model.totalCount();
        case OffsetNibble2State:
            return offsetNibble2Model.totalCount();
        case OffsetNibble3State:
            return offsetNibble3Model.totalCount();
        default:
            throw new RuntimeException();
        }
    }

    @Override
    public int pointToSymbol(int count) {
        //XXX HACK
        if (nextState != null) {
            state = nextState;
            nextState = null;
        }
        
        switch (state) {
        case LiteralState:
            int symbol = literalModel.pointToSymbol(count);
            if (symbol == SUBSTRING_SYMBOL) {
                nextState = State.LengthNibble0State;
            }
            return symbol;
        case LengthNibble0State:
            nextState = State.LengthNibble1State;
            return lengthNibble0Model.pointToSymbol(count);
        case LengthNibble1State:
            nextState = State.OffsetNibble0State;
            return lengthNibble1Model.pointToSymbol(count);
        case OffsetNibble0State:
            nextState = State.OffsetNibble1State;
            return offsetNibble0Model.pointToSymbol(count);
        case OffsetNibble1State:
            nextState = State.OffsetNibble2State;
            return offsetNibble1Model.pointToSymbol(count);
        case OffsetNibble2State:
            nextState = State.OffsetNibble3State;
            return offsetNibble2Model.pointToSymbol(count);
        case OffsetNibble3State:
            nextState = State.LiteralState;
            return offsetNibble3Model.pointToSymbol(count);
        default:
            throw new RuntimeException();
        }
    }

    @Override
    public void interval(int symbol, int[] result) {
        switch (state) {
        case LiteralState:
            literalModel.interval(symbol, result);
            if (symbol == SUBSTRING_SYMBOL) {
                state = State.LengthNibble0State;
            }
            break;
        case LengthNibble0State:
            state = State.LengthNibble1State;
            lengthNibble0Model.interval(symbol, result);
            break;
        case LengthNibble1State:
            state = State.OffsetNibble0State;
            lengthNibble1Model.interval(symbol, result);
            break;
        case OffsetNibble0State:
            state = State.OffsetNibble1State;
            offsetNibble0Model.interval(symbol, result);
            break;
        case OffsetNibble1State:
            state = State.OffsetNibble2State;
            offsetNibble1Model.interval(symbol, result);
            break;
        case OffsetNibble2State:
            state = State.OffsetNibble3State;
            offsetNibble2Model.interval(symbol, result);
            break;
        case OffsetNibble3State:
            state = State.LiteralState;
            offsetNibble3Model.interval(symbol, result);
            break;
        default:
            throw new RuntimeException();
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

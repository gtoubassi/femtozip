package org.toubassi.femtozip.encoding.offsetnibblefrequency;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.toubassi.femtozip.encoding.arithcoding.FrequencyCodeModel;

import com.colloquial.arithcode.ArithCodeModel;

public class OffsetNibbleFrequencyCodeModel implements ArithCodeModel {
    private enum State {
        LiteralLengthState, OffsetNibble0State, OffsetNibble1State, OffsetNibble2State, OffsetNibble3State;
    }
    
    private FrequencyCodeModel literalLengthModel;
    private FrequencyCodeModel offsetNibble0Model;
    private FrequencyCodeModel offsetNibble1Model;
    private FrequencyCodeModel offsetNibble2Model;
    private FrequencyCodeModel offsetNibble3Model;
    private State state = State.LiteralLengthState;
    private State nextState = null;

    public OffsetNibbleFrequencyCodeModel(FrequencyCodeModel literalLengthModel,
            FrequencyCodeModel offsetNibble0Model,
            FrequencyCodeModel offsetNibble1Model,
            FrequencyCodeModel offsetNibble2Model,
            FrequencyCodeModel offsetNibble3Model) {
        this.literalLengthModel = literalLengthModel;
        this.offsetNibble0Model = offsetNibble0Model;
        this.offsetNibble1Model = offsetNibble1Model;
        this.offsetNibble2Model = offsetNibble2Model;
        this.offsetNibble3Model = offsetNibble3Model;
    }

    public OffsetNibbleFrequencyCodeModel(DataInputStream in) throws IOException {
        literalLengthModel = new FrequencyCodeModel(in);
        offsetNibble0Model = new FrequencyCodeModel(in);
        offsetNibble1Model = new FrequencyCodeModel(in);
        offsetNibble2Model = new FrequencyCodeModel(in);
        offsetNibble3Model = new FrequencyCodeModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        literalLengthModel.save(out);
        offsetNibble0Model.save(out);
        offsetNibble1Model.save(out);
        offsetNibble2Model.save(out);
        offsetNibble3Model.save(out);
    }

    @Override
    public int totalCount() {
        switch (state) {
        case LiteralLengthState:
            return literalLengthModel.totalCount();
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
        case LiteralLengthState:
            int symbol = literalLengthModel.pointToSymbol(count);
            if (symbol > 255) {
                nextState = State.OffsetNibble0State;
            }
            return symbol;
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
            nextState = State.LiteralLengthState;
            return offsetNibble3Model.pointToSymbol(count);
        default:
            throw new RuntimeException();
        }
    }

    @Override
    public void interval(int symbol, int[] result) {
        switch (state) {
        case LiteralLengthState:
            literalLengthModel.interval(symbol, result);
            if (symbol > 255) {
                state = State.OffsetNibble0State;
            }
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
            state = State.LiteralLengthState;
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

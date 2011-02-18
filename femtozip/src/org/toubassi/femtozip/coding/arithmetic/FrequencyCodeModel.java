package org.toubassi.femtozip.coding.arithmetic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.colloquial.arithcode.ArithCodeModel;

public class FrequencyCodeModel implements ArithCodeModel {
    private int eofIndex;
    private int total;
    private int[] histogram;
    private short[] inverseHistogram;

    public static int[] computeHistogramWithEOFSymbol(byte[] data) {
        int[] histo = new int[256 + 1];
        for (int i = 0, count = data.length; i < count; i++) {
            histo[data[i]]++;
        }
        return histo;
    }

    private static int[] computeHistogram(short[] data, int maxSymbol) {
        int[] histo = new int[maxSymbol + 1 + 1];   // The array must accommodate [0...maxSymbol] plus the EOF.
        for (int i = 0, count = data.length; i < count; i++) {
            histo[data[i]]++;
        }
        return histo;
    }

    
    public FrequencyCodeModel() {
    }

    public FrequencyCodeModel(byte[] data, boolean allSymbolsSampled) {
        this(computeHistogramWithEOFSymbol(data), allSymbolsSampled);
    }
    
    public FrequencyCodeModel(short[] data, int maxSymbol, boolean allSymbolsSampled) {
        this(computeHistogram(data, maxSymbol), allSymbolsSampled);
    }

    public FrequencyCodeModel(int[] histogram, boolean allSymbolsSampled) {
        this(histogram, allSymbolsSampled, true);
    }
    
    public FrequencyCodeModel(int[] histogram, boolean allSymbolsSampled, boolean needsEOF) {
        int dataLength = 0;
        for (int i = 0, count = histogram.length; i< count; i++) {
            dataLength += histogram[i];
        }
        
        if (needsEOF) {
            eofIndex = histogram.length - 1;
            histogram[eofIndex] = 1;
        }
        else {
            eofIndex = -1;
        }
        
        this.histogram = histogram;
        this.total = dataLength + 1;
        if (!allSymbolsSampled) {
            for (int i = 0, count = histogram.length; i < count; i++) {
                if (histogram[i] == 0) {
                    histogram[i] = 1;
                    total++;
                }
            }
        }
        
        inverseHistogram = new short[total];
        
        for (int i = 0, j = 0, count = histogram.length; i < count; i++) {
            if (histogram[i] > 0) {
                Arrays.fill(inverseHistogram, j, j + histogram[i], (short)i);
                j += histogram[i];
            }
            if (i > 0) {
                histogram[i] += histogram[i - 1];
            }
        }
    }
    
    public FrequencyCodeModel(DataInputStream in) throws IOException {
        eofIndex = in.readInt();
        total = in.readInt();
        histogram = new int[in.readInt()];
        for (int i = 0, count = histogram.length; i < count; i++) {
            histogram[i] = in.readInt();
        }
        inverseHistogram = new short[in.readInt()];
        for (int i = 0, count = inverseHistogram.length; i < count; i++) {
            inverseHistogram[i] = in.readShort();
        }
    }
    
    public void save(DataOutputStream out) throws IOException {
        out.writeInt(eofIndex);
        out.writeInt(total);
        out.writeInt(histogram.length);
        for (int i = 0, count = histogram.length; i < count; i++) {
            out.writeInt(histogram[i]);
        }
        out.writeInt(inverseHistogram.length);
        for (int i = 0, count = inverseHistogram.length; i < count; i++) {
            out.writeShort(inverseHistogram[i]);
        }
    }
        
    public int totalCount() {
        return total;
    }

    @Override
    public int pointToSymbol(int count) {
        int symbol = inverseHistogram[count];
        return symbol == eofIndex ? EOF : symbol;
    }

    @Override
    public void interval(int symbol, int[] result) {
        
        if (symbol == EOF) {
            symbol = eofIndex;
        }
        result[0] = symbol == 0 ? 0 : histogram[symbol - 1];
        result[1] = histogram[symbol];
        result[2] = total;
    }

    public boolean escaped(int symbol) {
        return false;
    }

    public void exclude(int symbol) {
    }

    public void increment(int symbol) {
    }
}

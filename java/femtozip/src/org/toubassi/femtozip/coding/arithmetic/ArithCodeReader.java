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
package org.toubassi.femtozip.coding.arithmetic;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.colloquial.arithcode.ArithDecoder;
import com.colloquial.arithcode.ppm.ArithCodeModel;
import com.colloquial.io.BitInput;

public class ArithCodeReader {
    
    private ArithCodeReader(ArithDecoder decoder, ArithCodeModel model)
            throws IOException {
        _decoder = decoder;
        _model = model;
        decodeNextByte();
    }

    public ArithCodeReader(BitInput in, ArithCodeModel model)
            throws IOException {
        this(new ArithDecoder(in), model);
    }

    public ArithCodeReader(BufferedInputStream in, ArithCodeModel model)
            throws IOException {
        this(new BitInput(in), model);
    }

    public ArithCodeReader(InputStream in, ArithCodeModel model) throws IOException {
        this(new BitInput(new BufferedInputStream(in)), model);
    }

    public void close() throws IOException {
        _decoder.close();
    }

    public int readSymbol() throws IOException {
        int result = _nextByte;
        decodeNextByte();
        return result;
    }

    /**
     * The statistical model model on which the input stream is based.
     */
    private final ArithCodeModel _model;

    /**
     * The arithmetic decoder used to read bytes.
     */
    private final ArithDecoder _decoder;

    /**
     * The buffered next byte to write. If it's equal to -1, the end of stream
     * has been reached, otherwise next byte is the low order bits.
     */
    private int _nextByte;

    /**
     * Interval used for coding ranges.
     */
    private final int[] _interval = new int[3];

    private void decodeNextByte() throws IOException {
        if (_nextByte == ArithCodeModel.EOF)
            return;
        if (_decoder.endOfStream()) {
            _nextByte = ArithCodeModel.EOF;
            return;
        }
        while (true) {
            _nextByte = _model.pointToSymbol(_decoder
                    .getCurrentSymbolCount(_model.totalCount()));
            _model.interval(_nextByte, _interval);
            _decoder.removeSymbolFromStream(_interval);
            if (_nextByte != ArithCodeModel.ESCAPE)
                return;
        }
    }

}

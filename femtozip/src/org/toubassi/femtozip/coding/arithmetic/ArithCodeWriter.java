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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.colloquial.arithcode.ArithEncoder;
import com.colloquial.arithcode.ppm.ArithCodeModel;
import com.colloquial.io.BitOutput;

public class ArithCodeWriter {
    private ArithCodeWriter(ArithEncoder encoder, ArithCodeModel model) {
        _encoder = encoder;
        _model = model;
    }

    public ArithCodeWriter(BitOutput bitOut, ArithCodeModel model) {
        this(new ArithEncoder(bitOut), model);
    }

    public ArithCodeWriter(BufferedOutputStream out, ArithCodeModel model) {
        this(new BitOutput(out), model);
    }

    public ArithCodeWriter(OutputStream out, ArithCodeModel model) {
        this(new BitOutput(new BufferedOutputStream(out)), model);
    }

    public void close() throws IOException {
        encode(ArithCodeModel.EOF); // must code EOF to allow decoding to halt
        _encoder.close();
    }

    public void flush() throws IOException {
        _encoder.flush();
    }

    public void writeSymbol(int i) throws IOException {
        encode(i);
    }

    /**
     * The model on which the output stream is based.
     */
    private final ArithCodeModel _model;

    /**
     * The arithmetic encoder used to write coded bytes.
     */
    private final ArithEncoder _encoder;

    /**
     * Interval used for coding ranges.
     */
    private final int[] _interval = new int[3];

    private void encode(int symbol) throws IOException {
        while (_model.escaped(symbol)) {
            _model.interval(ArithCodeModel.ESCAPE, _interval); // have already
                                                               // done complete
                                                               // walk to
                                                               // compute escape
            _encoder.encode(_interval);
        }
        _model.interval(symbol, _interval); // have already done walk to element
                                            // to compute escape
        _encoder.encode(_interval);
    }

}

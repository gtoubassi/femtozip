package com.colloquial.arithcode;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** <P>A filter output stream which uses a statistical model and
 * arithmetic coding for compression of bytes read from an underlying
 * arithmetic encoder.  This encoder may be constructed from an output
 * stream or bit output.  Given a model and a stream, this class
 * operates in the same way as
 * <code>java.util.zip.GZIPOutputStream</code>.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see ArithCodeInputStream
 * @see ArithCodeModel
 * @since 1.0
 */
public class ArithCodeOutputStream extends OutputStream {

    /** Construct an output stream that writes to the specified output
     * events with the given arithmetic encoder with the given statistical model.
     * @param encoder Arithmetic encoder to use for coding output.
     * @param model Statistical model of byte stream.
     * @since 1.1
     */
    public ArithCodeOutputStream(ArithEncoder encoder, ArithCodeModel model) {
	_encoder = encoder;
	_model = model;
    }

    /** Construct an output stream that writes to the specified bit output
     * using arithmetic coding with the given statistical model.
     * @param bitOut Bit output to write coded bits to.
     * @param model Statistical model of byte stream.
     * @since 1.1
     */
    public ArithCodeOutputStream(BitOutput bitOut, ArithCodeModel model) {
	this(new ArithEncoder(bitOut), model);
    }
    
    /** Construct an output stream that writes to the specified buffered output
     * stream using arithmetic coding with the given statistical model.
     * @param model Statistical model of byte stream.
     * @param out  Buffered output stream to write coded bits to.
     * @since 1.1
     */
    public ArithCodeOutputStream(BufferedOutputStream out, ArithCodeModel model) {
	this(new ArithEncoder(out), model);
    }

    /** Construct an output stream that writes to the specified output
     * stream using arithmetic coding with the given statistical model.
     * @param output Output stream to write coded bits to.
     * @param model Statistical model of byte stream.
     */
    public ArithCodeOutputStream(OutputStream out, ArithCodeModel model) {
	this(new BufferedOutputStream(out), model); 
    }

    /** Close this output stream.
     * @throws IOException If there is an exception in the underlying encoder.
     */
    public void close() throws IOException {
	encode(ArithCodeModel.EOF);  // must code EOF to allow decoding to halt
	_encoder.close();
    }

    /** Flushes underlying stream.
     * @throws IOException If there is an exception flushing the underlying stream.
     */
    public void flush() throws IOException {
	_encoder.flush();
    }

    /** Writes array of bytes to the output stream.
     * @param bs Array of bytes to write.
     * @throws IOException If there is an exception in writing to the underlying encoder.
     */
    public void write(byte[] bs)  throws IOException {
	write(bs,0,bs.length);
    }
    
    /** Writes section of array of bytes to the output stream.
     * @param bs Array of bytes to write.
     * @param off Index from which to start writing.
     * @param len Number of bytes to write.
     * @throws IOException If there is an exception in writing to the underlying encoder.
     */
    public void write(byte[] bs, int off, int len)  throws IOException {
	while (off < len) write(Converter.byteToInteger(bs[off++]));
    }
    
    /** Writes the eight low-order bits of argument to the output stream
     * as a byte.
     * @param i Bits to write.
     * @throws IOException If there is an exception in writing to the underlying encoder.
     */
    public void write(int i)  throws IOException { 
	encode(i);
    }

    /** The model on which the output stream is based.
     */
    private final ArithCodeModel _model;

    /** The arithmetic encoder used to write coded bytes.
     */
    private final ArithEncoder _encoder;

    /** Interval used for coding ranges.
     */
    private final int[] _interval = new int[3];

    /** Writes encoded symbol after necessary escapes to the underlying
     * encoder.
     * @param symbol Symbol to encode.
     * @throws IOException If the underlying encoder throws an IOException.
     */
    private void encode(int symbol) throws IOException {
	while (_model.escaped(symbol)) {
	    _model.interval(ArithCodeModel.ESCAPE,_interval); // have already done complete walk to compute escape
	    _encoder.encode(_interval);
	}
	_model.interval(symbol,_interval); // have already done walk to element to compute escape
	_encoder.encode(_interval); 
    }

}

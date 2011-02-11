package com.colloquial.arithcode;

import java.io.OutputStream;
import java.io.IOException;

/** Writes to an underlying output stream a bit at a time.  A bit is can be input
 * as a boolean, with <code>true=1</code> and <code>false=0</code>, or
 * as a number, in which case any non-zero input will be converted to
 * <code>1</code>.  If the number of bits written before closing the output
 * does not land on a byte boundary, the remaining fractional byte is
 * filled with <code>0</code> bits.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see BitInput
 * @since 1.0
 */
public final class BitOutput {

    /** Construct a  bit output from the specified output stream.
     * @param out Underlying output stream.
     */
    public BitOutput(OutputStream out) {
	_out = out;
	reset();
    }

    /** Closes underlying output stream after filling to a byte
     * boundary with <code>0</code> bits.
     * @throws IOException If there is an I/O exception writing the next byte or closing the underlying output stream.
     */
    public void close() throws IOException {
	if (_nextBitIndex < 7)                       // there's something in the buffer
	    _out.write(_nextByte << _nextBitIndex); // shift to fill last byte
	_out.close();
    }

    /** Flushes the underlying output stream.
     * @throws IOException If there is an exception flushing the underlying output stream.
     */
    public void flush() throws IOException {
	_out.flush();
    }

    /** Writes the single specified bit to the underlying output stream,
     * <code>1</code> for <code>true</code> and <code>0</code> for <code>false</code>.
     * @param bit Value to write.
     * @throws IOException If there is an exception in the underlying output stream.
     */
    public void writeBit(boolean bit) throws IOException {
	if (bit) writeBitTrue();
	else writeBitFalse();
    }

    /** Writes a single <code>true</code> (<code>1</code>) bit.
     * @throws IOException If there is an exception in the underlying output stream.
     * @since 1.1
     */
    public void writeBitTrue() throws IOException {
	if (_nextBitIndex == 0) {
	    _out.write(_nextByte + 1);
	    reset();
	} else {
	    _nextByte = (_nextByte + 1) << 1;
	    --_nextBitIndex;
	}
    }

    /** Writes a single <code>false</code> (<code>0</code>) bit.
     * @throws IOException If there is an exception in the underlying output stream.
     * @since 1.1
     */
    public void writeBitFalse() throws IOException {
	if (_nextBitIndex == 0) {
	    _out.write(_nextByte);
	    reset();
	} else {
	    _nextByte <<= 1;
	    --_nextBitIndex;
	}
    }

    /** Buffering for output. Bytes are represented as integers,
     * primarily for efficiency of bit fiddling and for compatibility
     * with underlying output stream.
     */
    private int _nextByte;

    /** The indexof the next bit to write into the next byte.
     */
    private int _nextBitIndex;

    /** Underlying output stream.
     */
    private final OutputStream _out;

    /** Resets the bit buffer.
     */
    private void reset() {
	_nextByte = 0;
	_nextBitIndex = 7;
    }


}

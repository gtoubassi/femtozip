package com.colloquial.arithcode;

import java.io.InputStream;
import java.io.IOException;

/** Reads input from an underlying input stream a bit at a time.  Bits
 * are returned as booleans, with <code>true=1</code> and
 * <code>false=0</code>.
 *
 * @see com.colloquial.arithcode.BitOutput
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see BitOutput
 * @since 1.0
 */
public final class BitInput {

    /** Constructs a bit input from an underlying input stream.
     * @param in Input stream backing this bit input.
     * @throws IOException If there is an exception reading from the specified input stream.
     */
    public BitInput(InputStream in) throws IOException {
	_in = in;
	readAhead();
    }

    /** Returns number of bits available for reading.  Will always be
     * <code>0</code> or <code>1</code>.
     * @return Number of bits available for reading.
     * @throws IOException If there is an exception checking available bytes in the underlying input stream.
     */
    public long available() throws IOException {
	return endOfStream() ? 0 : 1;
    }

    /** Closes the underlying input stream.
     * @throws IOException If there is an exception closing the underlying input stream.
     */
    public void close() throws IOException { 
	_in.close(); 
    }

    /** Returns <code>true</code> if all of the available bits have been read.
     * @return <code>true</code> if all of the available bits have been read.
     */
    public boolean endOfStream() { return _endOfStream; }

    /** Reads the next bit from the input stream.  Returns garbage if reading
     * while available() is false.
     * @return The boolean value of the next bit, <code>true</code>=1, <code>false</code>=0.
     * @throws IOException If there is an exception reading a byte from the underlying stream.
     */
    public boolean readBit() throws IOException {
	if (_nextBitIndex > 0) 
	    return ((_nextByte & (1 << _nextBitIndex--)) != 0); // inspects bit in buffered byte
	boolean result = ((_nextByte & 1) != 0); // on last bit in byte; buffer new byte
	readAhead();
	return result;
    }

    /** Underlying input stream.
     */
    private final InputStream _in;
    
    /** Buffered byte from which bits are read.
     */
    private int _nextByte; // implied = 0;
    
    /** Position of next bit in the buffered byte.
     */
    private int _nextBitIndex;

    /** Set to true when all bits have been read.
     */
    private boolean _endOfStream = false;

    /** Reads the next byte from the input stream into <code>_nextByte</code>.
     * @throws IOException If there is an IOException reading from the stream.
     */
    private void readAhead() throws IOException {
	if (_endOfStream) return;
	_nextByte = _in.read(); 
	if (_nextByte == -1) { _endOfStream = true; return; }
	_nextBitIndex = 7;
    }

}

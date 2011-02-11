package com.colloquial.arithcode;

/** <P>Stores a queue of bytes in a buffer with a maximum size.  New
 * bytes are added to the tail of the queue, and if the size exceeds
 * the maximum, bytes are removed from the front of the queue.  Used
 * to model a sliding window of a fixed width over a stream of bytes
 * presented a byte at a time.  The bytes in the current window are
 * accessed through an array of bytes, an offset and a length.  
 * 
 * <P>For instance, with a maximum length of 2, beginning with an
 * empty buffer and adding bytes 1, 2, 3, and 4 in that order leads to
 * queues <code>{1}</code>, <code>{1,2}</code>, <code>{2,3}</code> and
 * <code>{3,4}</code>.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.0
 * @since 1.0
 */ 
public final class ByteBuffer {

    /** Construct a context buffer of given maximum size.
     * @param maxWidth Maximum number of bytes in a context.
     */
    public ByteBuffer(int maxWidth) {
	_maxWidth = maxWidth;
	_bytes = new byte[BUFFER_SIZE_MULTIPLIER * maxWidth];
    }

    /** Current array of bytes backing this byte buffer.  The returned
     * bytes are not a copy and should not be modified.
     * @return Array of bytes backing this buffer.
     */
    public byte[] bytes() {
	return _bytes;
    }

    /** Current offset of this buffer into the byte array.
     * @return Offset of the buffer into the byte array.
     */
    public int offset() {
	return _offset;
    }

    /** Current length of this buffer.  
     * @return Length of this buffer.
     */
    public int length() {
	return _length;
    }

    /** Add a byte to the end of the context, removing first element if
     * necessary.
     * @param b Byte to push onto the tail of the context.
     */
    public void buffer(byte b) {
	if (nextFreeIndex() > maxIndex()) tampDown();
	_bytes[nextFreeIndex()] = b;
	if (_length < _maxWidth) ++_length;
	else ++_offset;
    }

    /** Return a string representation of this context using
     * the current localization to convert bytes to characters.
     * @return String representation of this context.
     */
    public String toString() {
	return new String(_bytes,_offset,_length);
    }

    /** Array of bytes used to buffer incoming bytes.
     */
    final byte[] _bytes;

    /** Maximum number of bytes in queue before adding pushes one off.
     */
    private final int _maxWidth;

    /** Offset of first byte of current context in buffer.
     */
    int _offset = 0;

    /** Number of bytes in the context.  Maximum will be given during construction.
     */
    int _length = 0;

    /** Index in the buffer for next element. May point beyond the
     * maximum index if there is no more space.
     * @return Index for next element.
     */
    private int nextFreeIndex() {
	return _offset+_length;
    }

    /** The maximum index in the buffer.
     * @param Index of last element in the buffer.
     */
    private int maxIndex() {
	return _bytes.length-1;
    }

    /** Moves bytes in context down to start of buffer.
     */
    private void tampDown() {
	for (int i = 0; i < _length-1; ++i) { 
	    _bytes[i] = _bytes[_offset+i+1]; 
	}
	_offset = 0;
    }

    /** Number of contexts that fit in the buffer without shifting.
     */
    private static final int BUFFER_SIZE_MULTIPLIER = 32;

}

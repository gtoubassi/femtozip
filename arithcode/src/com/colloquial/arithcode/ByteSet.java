package com.colloquial.arithcode;

/** A set of bytes.  Supports add operations, containment queries,
 * and may be cleared.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @since 1.0
 */
public final class ByteSet {
    
    /** Construct a byte set.
     */
    public ByteSet() {
	// clear(); implied
    }

    /** Remove all of the bytes from this set.
     */
    public void clear() {
	_bits1 = 0;
	_bits2 = 0;
	_bits3 = 0;
	_bits4 = 0;
    }

    /* Adds a byte, specified as an integer, to this set.
     * @param i Byte to add to this set.
     * @since 1.1
     */
    public void add(int i) { add(Converter.integerToByte(i)); }

    /** Add a byte to this set.
     * @param b Byte to add to this set.
     */
    public void add(byte b) {
	if      (b >  63) _bits4 |= ((long) 1) << (- 64 + (int) b);
	else if (b >=  0) _bits3 |= ((long) 1) << (/* 0 +*/(int) b);
	else if (b > -65) _bits2 |= ((long) 1) << (  64 + (int) b); 
	else              _bits1 |= ((long) 1) << ( 128 + (int) b);
    }

    /** Removes a byte, specified as an integer, from this set.
     * @param i Integer specification of byte to remove from this set.
     * @since 1.1
     */
    public void remove(int i) {  add(Converter.integerToByte(i)); }

    /** Removes a byte from this set.
     * @param b Byte to remove from this set.
     * @since 1.1
     */
    public void remove(byte b) {
	if      (b >  63) _bits4 &= ~(((long) 1) << (- 64 + (int) b));
	else if (b >=  0) _bits3 &= ~(((long) 1) << (/* 0 +*/(int) b));
	else if (b > -65) _bits2 &= ~(((long) 1) << (  64 + (int) b)); 
	else              _bits1 &= ~(((long) 1) << ( 128 + (int) b));

    }

    /** Adds all the members of specified set to this set. The result is
     * that this set's value is the union of its previous value with the
     * specified set.  The argument set is unchanged.
     * @param that Byte set to add to this set.
     * @since 1.1
     */
    public void add(ByteSet that) {
	_bits1 |= that._bits1;
	_bits2 |= that._bits2;
	_bits3 |= that._bits3;
	_bits4 |= that._bits4;
    }

    /** Removes all the members of specified set from this set. The result is
     * that this set's value is the complement of its previous value with the
     * specified set.  The argument set is unchanged.
     * @param that Byte set to remove from this set.
     * @since 1.1
     */
    public void remove(ByteSet that) {
	_bits1 &= ~that._bits1;
	_bits2 &= ~that._bits2;
	_bits3 &= ~that._bits3;
	_bits4 &= ~that._bits4;
    }

    /** Removes all the elements of this set that are not in the specified
     * set.  The result is that this set's value is the intersection of its
     * previous value with the specified set.  The argument set is unchagned.
     * @param that Byte set to restrict this set to.
     * @since 1.1
     */
    public void restrict(ByteSet that) {
	_bits1 &= that._bits1;
	_bits2 &= that._bits2;
	_bits3 &= that._bits3;
	_bits4 &= that._bits4;
    }

    /** Returns <code>true</code> if byte specified as an integer
     * is a member of this set.  Conversion is done by casting.
     * @param i Integer representation of byte to be tested for membership.
     * @return <code>true</code> if the specified byte is a member of this set.
     * @since 1.0
     */
    public boolean contains(int i) {
	return contains(Converter.integerToByte(i));
    }

    /** Returns <code>true</code> if specified byte is a member
     * of this set.
     * @param b Byte to test for membership in this set.
     * @return <code>true</code> if the specified byte is a member of this set.
     */
    public boolean contains(byte b) {
	// built-in ASCII order preference in first two test cases
	// moving 0 inside ternary is *much* slower -- must be set up for ints, not bools
	if (b > 63) return 0 != (_bits4 & ((long) 1) << (- 64 + (int) b));
	if (b >= 0) return 0 != (_bits3 & ((long) 1) << ((int) b));
	if (b > -65) return 0 != (_bits2 & ((long) 1) << (  64 + (int) b));
	return 0 != ((_bits1 & ((long) 1) << ( 128 + (int) b)));
    }

    /** Returns number of elements in this set.
     * @return Number of elements in this set.
     */
    public int size() {
	int sum = 0;
	for (int i = 0; i < 256; ++i) if (contains(i)) ++sum;
	return sum;
    }

    /** An empty set.  Unsafe, because nothing prevents
     * the addition of elements. So it's kept to the package.
     * The decision to do it this way was to keep ByteSet itself a final
     * class for the sake of efficiency.
     */
    static final ByteSet EMPTY_SET = new ByteSet(); 

    /** Representation of bytes -128..-65.
     * Longs are a real kick in the butt for 1.4 non-server.
     */
    private long _bits1; // -128..-65 

    /** Representation of bytes -64..-1.
     */
    private long _bits2; 

    /** Representation of bytes // 0..63.
     */
    private long _bits3; 

    /** Representation of bytes 64..127.
     */
    private long _bits4; 

}
	

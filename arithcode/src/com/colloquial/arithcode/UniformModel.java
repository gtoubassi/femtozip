package com.colloquial.arithcode;

/** <P>A singleton uniform distribution byte model.  Provides a single
 * static member that is a non-adaptive model assigning equal
 * likelihood to all 256 bytes and the end-of-file marker.  This will
 * require approximately -log<sub>2</sub> 1/257 ~ 8.006, bits per symbol,
 * including the end-of-file symbol.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.0
 * @since 1.0
 */
public final class UniformModel implements ArithCodeModel {

    // specified by ArithCodeModel
    public int totalCount() {
	return NUM_OUTCOMES;
    }
     
    // specified by ArithCodeModel
    public int pointToSymbol(int midCount) {
	return (midCount == EOF_INDEX ? EOF : midCount);
    }

    // specified by ArithCodeModel
    public void interval(int symbol, int[] result) {
	result[0] = symbol == EOF ? EOF_INDEX : symbol;
	result[1] = result[0] + 1;
	result[2] = NUM_OUTCOMES;
    }

    // specified by ArithCodeModel
    public boolean escaped(int symbol) { return false; }

    // specified by ArithCodeModel
    public void exclude(int symbol) {  }

    // specified by ArithCodeModel
    public void increment(int symbol) { }

    /** A re-usable uniform model. 
     */
    public final static UniformModel MODEL = new UniformModel();

    /** Construct a uniform model.
     */
    private UniformModel() { }

    /** Total number of bytes.
     */
    private static final int NUM_BYTES = 256;
    
    /** Index in the implicit count array for the end-of-file outcome.
     */
    private static final int EOF_INDEX = 256;
    
    /** Index in the count array for the cumulative total
     * of all outcomes.
     */
    private static final int NUM_OUTCOMES=NUM_BYTES+1;
    
}

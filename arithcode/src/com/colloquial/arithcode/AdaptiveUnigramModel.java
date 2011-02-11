package com.colloquial.arithcode;

/** <P>Provides an adaptive model based on bytes observed in the input
 * stream.  Each byte count is initialized at <code>1</code> and
 * incremented by <code>1</code> for each instance seen. If
 * incrementing an outcome causes the total count to exceed
 * <code>MAX_COUNT</code>, then all counts are divided by 2 and
 * rounded up.  Estimation is by frequency (also known as a maximum
 * likelihood estimate).
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @since 1.0
 */
public final class AdaptiveUnigramModel implements ArithCodeModel {

    /** Construct an adaptive unigram model, initializing all byte counts
     * and end-of-file to <code>1</code>.
     */
    public AdaptiveUnigramModel() {                           // initial cumulative counts
	for (int i = 0; i < NUM_BYTES; ++i)  _count[i] = i;   // low[i]   = high[i+1]
	_count[EOF_INDEX] = EOF_INDEX;                        // low[EOF] = high[255]
	_count[TOTAL_INDEX] = TOTAL_INDEX;                    // total    = high[EOF]
    }

    // specified in ArithCodeModel
    public void interval(int symbol, int[] result) {
	if (symbol == EOF) symbol = EOF_INDEX;
	result[0] = lowCount(symbol);
	result[1] = highCount(symbol);
	result[2] = totalCount();
	increment(symbol);
    }

    // specified in ArithCodeModel
    public int pointToSymbol(int midCount) {
	int low = 0;
	int high = TOTAL_INDEX;
	while (true) { // binary search returns when it finds result
     	    int mid = (high+low)/2;
	    if (_count[mid] > midCount) { 
		if (high == mid) --high;
		else high = mid; 
	    } else if (_count[mid+1] > midCount) {
		return (mid==EOF_INDEX) ? EOF : mid;
	    } else { 
		if (low==mid) ++low;
		else low = mid;
	    }
	}
    }

    // specified in ArithCodeModel
    public int totalCount() { return _count[TOTAL_INDEX]; }

    // specified in ArithCodeModel
    public boolean escaped(int symbol) { return false; }

    // specified in ArithCodeModel
    public void exclude(int i) { }

    // specified by ArithCodeModel
    public void increment(int i) {
	while (++i <= TOTAL_INDEX) ++_count[i];
	if (totalCount() >= MAX_COUNT) rescale();
    }
     
    /** Counts for each outcome. Indices 0 to 255 for the
     * usual counts, 256 for end-of-file, and 257 for total.
     * Each outcome i between 0-256 is coded by interval 
     * (_count[i],_count[i+1],_count[257]).
     */
    private int[] _count = new int[258];

    /** The cumulative count of all outcomes below given outcome.
     * @param i Index of given outcome.
     * @return Low count of interval for given symbol.
     */
    private int lowCount(int i) { return _count[i]; }

    /** The cumulative count of all outcomes below given outcome plus
     * the count of the outcome.
     * @param i Index of given outcome.
     * @return High count of interval for given symbol.
     */
    private int highCount(int i) { return _count[i+1]; }

    /** Rescale the counts by adding 1 to all counts and dividing by
     * <code>2</code>.
     */
    private void rescale() {
	int[] freqs = new int[_count.length];
	for (int i = 1; i < freqs.length; ++i)
	    freqs[i] = (_count[i] - _count[i-1] + 1) / 2;  // compute from cumulative; round up
	for (int i = 1; i < _count.length; ++i)            // compute cumulative;
	    _count[i] = _count[i-1] + freqs[i];            // _count[0] = 0 is implicit
    }

    /** Maximum count before rescaling.
     */
    private static final int MAX_COUNT = 64*1024;
    
    /** Total number of bytes.
     */
    private static final int NUM_BYTES = 256;

    /** Index in the count array for the end-of-file outcome.
     */
    private static final int EOF_INDEX = 256;
    
    /** Index in the count array for the cumulative total of all
     * outcomes.
     */
    private static final int TOTAL_INDEX = 257;

}

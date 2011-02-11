package com.colloquial.arithcode;

/** Package class for use by the PPMModel.  A fragmentary adaptive
 * unigram model that allows exclusions in converting points to
 * intervals and vice-versa.  One such model will be used for each
 * unigram context.  
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @since 1.1
 */
final class ExcludingAdaptiveUnigramModel {

    /** Construct an excluding adaptive unigram model.
     */
    public ExcludingAdaptiveUnigramModel() {
	_count = new int[257];
	java.util.Arrays.fill(_count,1); // counts are non-cumulative
    }

    /** Compute the resulting interval to code the specified symbol given
     * the specified excluded bytes.
     * @param symbol Symbol to code.
     * @param result Interval to code the symbol.
     * @param exclusions Bytes to exclude as possible outcomes for interval.
     */
    public void interval(int symbol, int[] result, ByteSet exclusions) {
	if (symbol == ArithCodeModel.EOF) symbol = EOF_INDEX;
	int sum = 0; 
	for (int i = 0; i < symbol; ++i) if (!exclusions.contains(i)) sum += _count[i];
	result[0] = sum;
	sum += _count[symbol];
	result[1] = sum;
	for (int i = symbol+1; i < _count.length-1; ++i) if (!exclusions.contains(i)) sum += _count[i];
	if (symbol != EOF_INDEX) sum += _count[EOF_INDEX];
	result[2] = sum;
	increment(symbol);
    }

    /** Return the symbol corresponding to the specified count, given
     * the specified excluded bytes.
     * @param midCount Count of symbol to return.
     * @param exclusions Bytes to exclude from consideration.
     * @return Symbol represented by specified count.
     */
    public int pointToSymbol(int midCount, ByteSet exclusions) {
	int sum = 0; 
	for (int mid = 0; ; ++mid) {
	    if (mid != EOF_INDEX && exclusions.contains(mid)) continue;
	    sum += _count[mid];
	    if (sum > midCount) return (mid == EOF_INDEX) ? ArithCodeModel.EOF : mid;
	}
    }

    /** Total count for interval given specified set of exclusions.
     * @param exclusions Bytes to exclude as outcomes.
     * @return Total count of all non-excluded outcomes.
     */
    public int totalCount(ByteSet exclusions) {
	int total = 0;
	for (int i = 0; i < _count.length; ++i) 
	    if (i == EOF_INDEX || !exclusions.contains(i)) total += _count[i];
	return total;
    }

    /** Increment the count for the given outcome. 
     * @param i Outcome to increment
     */
    public void increment(int i) {
	if (++_count[i] > MAX_INDIVIDUAL_COUNT) rescale();
    }

    /** Counts for each outcome. Indices 0 to 255 for the
     * usual counts, 256 for end-of-file, and 257 for total.
     */
    private int[] _count;

    /** Rescale the counts by dividing all frequencies by 2, but
     * taking a minimum of 1.
     */
    private void rescale() {
	for (int i = 0; i < _count.length; ++i) _count[i] = (_count[i] + 1)/2;
    }
     
    /** Maximum count before rescaling.
     */
    private static final int MAX_INDIVIDUAL_COUNT = 8*1024;
    
    /** Index in the count array for the end-of-file outcome.
     */
    private static final int EOF_INDEX = 256;
    


}

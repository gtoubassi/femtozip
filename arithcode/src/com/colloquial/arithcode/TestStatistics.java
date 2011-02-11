package com.colloquial.arithcode;

/** Package local helper class to compute statistics for a single
 * compression experiment.  Stores original and coded bytes, average
 * and cumulative compression and speeds.  Pretty printing is through
 * toString.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @see TestSet
 * @version 1.1
 * @since 1.1
 */
class TestStatistics implements Comparable {

    // specified in Comparable
    public int compareTo(Object that) {
	return compareTo((TestStatistics) that);
    }

    // specified in Object
    public String toString() {
	StringBuffer report = new StringBuffer();
	report.append("\n");
	report.append("TEST: " + _name);
	report.append("\n");
	report.append("Overall Compression Rate " + roundOff(8.0 * rate(_totalCodedBytes,_totalOriginalBytes)) + " b/B");
	report.append("\n");
	report.append("Overall Encode Speed     " + roundOff(rate(_totalOriginalBytes, _totalEncodeTime)) + " kB/s");
	report.append("\n");
	report.append("Overall Decode Speed     " + roundOff(rate(_totalOriginalBytes, _totalDecodeTime)) + " kB/s");
	report.append("\n");
	report.append("Average Compression Rate " + roundOff(8.0 * _totalCompressionRate / (double) _numTests));
	report.append("\n");
	report.append("Average Encode Speed     " + roundOff(_totalEncodeSpeed / (double) _numTests));
	report.append("\n");
	report.append("Average Decode Speed     " + roundOff(_totalDecodeSpeed / (double) _numTests));
	return report.toString();
    }

    TestStatistics(String name) { _name = name; }

    /** Record the result of an experiment with specified number of original and encoded
     * bytes with specified encode and decode time.
     * @param originalBytes Number of bytes in original source.
     * @param codedBytes Number of bytes in decoded result.
     * @param encodeTime Time taken to do encoding in milliseconds.
     * @param decodeTime Time taken to do decoding in milliseconds.
     */
    void record(int originalBytes, int codedBytes, long encodeTime, long decodeTime) {
	++_numTests;
	_totalOriginalBytes += originalBytes;
	_totalCodedBytes += codedBytes;
	_totalEncodeTime += encodeTime;
	_totalDecodeTime += decodeTime;
	_totalCompressionRate += rate(codedBytes,originalBytes); 
	_totalEncodeSpeed += rate(originalBytes,encodeTime);
	_totalDecodeSpeed += rate(originalBytes,decodeTime);
    }

    /** Used for a line of reporting by TestSet.
     * @return The string representation of the statistics for this test on one line.
     */
    String lineReport() {
	return (_name 
		+ roundOff(8.0 * rate(_totalCodedBytes,_totalOriginalBytes),8,2) 
		+ roundOff(rate(_totalOriginalBytes, _totalEncodeTime),8,0)
		+ roundOff(rate(_totalOriginalBytes, _totalDecodeTime),8,0)
		+ roundOff(8.0 * _totalCompressionRate / (double) _numTests,8,2)
		+ roundOff(_totalEncodeSpeed / (double) _numTests,8,0)
		+ roundOff(_totalDecodeSpeed / (double) _numTests,8,0));
    }


    /** Name of test.
     */
    private String _name;

    /** Cumulative number of original bytes.
     */
    private int _totalOriginalBytes;

    /** Cumulative number of coded bytes.
     */
    private int _totalCodedBytes;

    /** Number of tests that have been run.
     */
    private int _numTests;

    /** Total of KB/s rate for all encodings (for average). 
     */
    private double _totalEncodeSpeed;

    /** Total of KB/s rate for all decodings (for average). 
     */
    private double _totalDecodeSpeed;

    /** Running total of sum of compression rates per file (for average).
     */
    private double _totalCompressionRate;

    /** Total amount of time in milliseconds spent encoding.
     */
    private long _totalEncodeTime;

    /** Total amount of time in milliseconds spent decoding.
     */
    private long _totalDecodeTime;

    /** Compare using average compression rates, breaking ties by name.
     * Assumes tests have been run on same set.
     */
    private int compareTo(TestStatistics that) {
	if (_totalCompressionRate < that._totalCompressionRate) return 1;
	if (_totalCompressionRate > that._totalCompressionRate) return -1;
	return _name.compareTo(that._name);
    }

    /** Rate of first argument divided by second argument as doubles.
     * @param a Numerator.
     * @param b Denominator.
     * @return Numerator over denominator with double division.
     */
    private static double rate(long a, long b) {
	return ((double) a) / (double) b;
    }

    /** Round off a double to 2 decimal places, taking up 8 total characters.
     * @param x Double to round off and convert to string.
     * @return Result of rounding as string.
     */
    private static String roundOff(double x) {
	return roundOff(x,8,2);
    }

    /** Round off a double to specified number of decimal places, taking up specified total width
     * @param x Double to round off and convert to string.
     * @param width Width of final string representation.
     * @param numDecimalPlaces Number of decimal places in representation.
     * @return Result of rounding as string.
     */
    private static String roundOff(double x, int width, int numDecimalPlaces) {
	double factor = 1.0;
	for (int i = 0; i < numDecimalPlaces; ++i) factor *= 10.0;
	return roundOff(x,factor,width);
    }

    /** Round off a double with a multiplicative factor, padding to specified width.
     * @param x Double to round off and convert to string.
     * @param width Width of final string representation.
     * @param factor 10 to the number of decimal places to keep.
     * @return Result of rounding as string.
     */
    private static String roundOff(double x, double factor, int width) {
	int intRep = (int) (factor * x);
	String result = "" + ((double)((int) (factor * x)))/factor;
	for (int i = 0; i < 1; ++i) {
	    if (intRep % 10 != 0) break;
	    result = result + "0";
	    intRep = intRep / 10;
	}
	if (factor == 1.0) result = ""+(int)x;
	while (result.length() < width) result = " " + result;
	return result;
    }

}

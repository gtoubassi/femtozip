package com.colloquial.arithcode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

/** Package local helper class to compute statistics for a set
 * of compression experiments.  Stores original and coded
 * bytes, average and cumulative compression and speeds.
 * Pretty printing is through report.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @see TestStatistics
 * @version 1.1
 * @since 1.1
 */
class TestSet {

    /** Records an outcome for a compression for a given test name,
     * specifying original and coded bytes, along with encode and decode times.
     * @param name Name of test.
     * @param originalBytes Number of bytes in input.
     * @param codedBytes Number of bytes after compression.
     * @param encodeTime Number of milliseconds required to encode.
     * @param decodeTime Number of milliseconds required to decode.
     */
    void record(String name, int originalBytes, int codedBytes, long encodeTime, long decodeTime) {
	if (!_tests.containsKey(name)) _tests.put(name,new TestStatistics(name));
	((TestStatistics) _tests.get(name)).record(originalBytes,codedBytes,encodeTime,decodeTime);
    }

    // specified by Object
    public String toString() {
	TreeSet results = new TreeSet();
	Iterator it = _tests.keySet().iterator();
	while (it.hasNext()) results.add(_tests.get(it.next()));

	StringBuffer sb = new StringBuffer();
	sb.append("\n");
	sb.append("\n");
	sb.append("            TOTAL                   AVERAGE");
	sb.append("\n");
	sb.append("NAME        b/B   Encode  Decode    b/B   Encode  Decode");
	sb.append("\n-----------------------------------------------------------");
	Iterator resultsIterator = results.iterator(); // sort
	while (resultsIterator.hasNext()) {
	    sb.append("\n");
	    sb.append(((TestStatistics) resultsIterator.next()).lineReport());
	}
	return sb.toString();
    }

    /** Storage for all of the tests.
     */
    private HashMap _tests = new HashMap();

    /** Clears the test set by removing all results. 
     */
    void clear() { _tests.clear(); }

}

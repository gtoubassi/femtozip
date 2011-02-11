package com.colloquial.arithcode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;

/** Runs test suite for arithmetic coding and decoding with all of th esupplied
 * compression models from {@link #main}. Behavior is specified with
 * the following arguments.
 * <UL>
 * <LI><code>-f   <i>FileName</i>: </code> 
 *     Test specified file.</LI>
 * <LI><code>-s   <i>Integer</i>: </code>
 *     Sized tests up to specified number of bytes.</LI>
 * <LI><code>-g: </code>
 *     Run small tests.</LI>
 * <LI><code><i>String</i>: </code>
 *     Test specified string.</LI>
 * <LI><code>-c   <i>Directory</i>: </code>
 *     Test calgary corpus found in specified directory.</LI>
 * <LI><code>-x   <i>Directory</i>: </code>
 *     Test xml corpus found in specified directory.</LI>
 * </UL> 
 * <P>
 * The Calgary corpus can be downloaded from:
 * <blockquote>
 *   <a href="ftp://ftp.cpsc.ucalgary.ca/pub/projects/text.compression.corpus">
 *     ftp://ftp.cpsc.ucalgary.ca/pub/projects/text.compression.corpus
 *   </a>.
 * </blockquote>
 * </P>
 * <P>
 * Because of the use of statics, only a single test should be run per virtual machine.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see ArithCodeModel
 * @see ArithCodeInputStream
 * @see ArithCodeOutputStream
 * @see AdaptiveUnigramModel
 * @see UniformModel
 * @see PPMModel
 * @since 1.0
 */
public final class Test {

    /** Runs test suite as specified by arguments.
     * <UL>
     * <LI><code>-f   <i>FileName</i>: </code> 
     *     Test specified file.</LI>
     * <LI><code>-s   <i>Integer</i>: </code>
     *     Sized tests up to specified number of bytes.</LI>
     * <LI><code>-g: </code>
     *     Run small tests.</LI>
     * <LI><code><i>String</i>: </code>
     *     Test specified string.</LI>
     * <LI><code>-c   <i>Directory</i>: </code>
     *     Test calgary corpus found in specified directory.</LI>
     * <LI><code>-x   <i>Directory</i>: </code>
     *     Test James Cheney's XML corpus found in specified directory.</LI>
     * </UL> 
     * @param args Parameters in fixed order.
     * @throws IOException If there is an underlying I/O exception during compression/decompression.
     */
    public static void main(String[] args) throws IOException {
	System.out.println();
	System.out.println("Start Time: " + new Timestamp(System.currentTimeMillis()));
	long startTime = System.currentTimeMillis();
	_testSet.clear();
	for (int i = 0; i < args.length; ++i) {
	    if (args[i].equals("-f")) test(new File(args[++i]));
	    else if (args[i].equals("-s")) testSize(Integer.parseInt(args[++i]));
	    else if (args[i].equals("-c")) testCalgary(args[++i]);
	    else if (args[i].equals("-x")) testXML(args[++i]);
	    else if (args[i].equals("-g")) testFixed();
	    else test(args[i]);
	}
	System.out.println();
	System.out.print("Total Time: " + timeToSeconds(elapsed(startTime)));
	System.out.println(_testSet);
    }


    /** Read all of the input from the given input stream and write
     * it to the given output stream.
     * @param in Input stream from which to read.
     * @param out Output stream to which to write.
     * @throws IOException If there is an exception reading or writing on the given streams.
     */
    static void copyStream(InputStream in, OutputStream out) throws IOException {
	while (true) {
	    int j = in.read();
	    if (j == -1) {
		in.close();
		out.close();
		return;
	    }
	    out.write(j);
	}
    }

    /** Return elapsed time since specified time in milliseconds (1/1000 second).
     * @param start Time from which to measure.
     * @return Time since start time in milliseconds.
     */
    static long elapsed(long start) { return System.currentTimeMillis() - start; }

    /** Convert specified time in milliseconds to a string in seconds.
     * @param t Time to convert to a string.
     * @return String representation of specified time.
     */
    static String timeToSeconds(long t) { return ((double)t)/1000.0 + " seconds"; }

    /** Creates the test set to use for the tests.
     */
    private static TestSet _testSet = new TestSet();

    /** Hide unused constructor.
     */
    private Test() { }

    /** Runs tests from 1 to give size, increasing size by a factor
     * of two at each step.  For each size, a test is made of a constant
     * string consisting of repetitions of a single character, and a test of a random sequence of
     * letters and then a random sequence of bytes.
     * @param size Maximum size up to which to test.
     * @throws IOException If there is an underlying I/O exception during compression/decompression.
     */
    private static void testSize(int size) throws IOException {
	StringBuffer constantSB = new StringBuffer("a");
	Random random = new Random();
	for (int k = 1; k <= size; k *= 2) {
	    byte[] bs = new byte[k];
	    nextRandomAlphaNum(bs,random);
	    test(bs);
	    random.nextBytes(bs);
	    test(bs);
	    String constantString = constantSB.toString();
	    test(constantString.toString());
	    constantSB.append(constantSB.toString());
	}
    }

    /** Fixed test suite.  
     * @throws IOException If there is an underlying I/O exception during compression/decompression.
     */
    private static void testFixed() throws IOException {
	test("");
	test("The quick brown fox jumped over the lazy dog.");
	test("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	test("01234567890~`!@#$%^&*()-_=+{[}]:;\"'<,>.?/|\\." + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.");
    }

    /** Runs a test on James Cheney's XML corpus.
     * @param path Name of directory in which to find the Calgary corpus.
     * @throws IOException If there is an underlying I/O exception during compression/decompression.
     */
    private static void testXML(String path) throws IOException {
     	                                         // Description [name in Cheney's paper]
	test(new File(path,"play1.xml"));        // Shakespearean play [play]
	// test(new File(path,"play2.xml"));
	// test(new File(path,"play3.xml"));

	test(new File(path,"treebank.xml"));     // natural language parses [treebank]

	// test(new File(path,"w3c1.xml"));      // xml spec from w3c
	test(new File(path,"w3c2.xml"));         // [spec]
	test(new File(path,"w3c3.xml"));         // [spec2]
	// test(new File(path,"w3c4.xml"));
	// test(new File(path,"w3c5.xml"));

	test(new File(path,"weblog.xml"));       // web log [weblog]

	test(new File(path,"tpc.xml"));          // (small)  [tpc?]

	test(new File(path,"sprot.xml"));        // (small)  [sprot]

	test(new File(path,"elts.xml"));         // statistical & scientific db [elts]
	test(new File(path,"stats1.xml"));       // [stats]
	// test(new File(path,"stats2.xml"));
	
	// test(new File(path,"pcc1.xml"));      // formal proof
	test(new File(path,"pcc2.xml"));         // [proof]
	// test(new File(path,"pcc3.xml"));

	test(new File(path,"tal1.xml"));         // annotated assembly language [tal]
	// test(new File(path,"tal2.xml"));
	// test(new File(path,"tal3.xml"));
    }

    /** Runs a test on the Calgary corpus.
     * @param path Name of directory in which to find the Calgary corpus.
     * @throws IOException If there is an underlying I/O exception during compression/decompression.
     */
    private static void testCalgary(String path) throws IOException {
	test(new File(path,"progc"));   // programs
	test(new File(path,"progl"));
	test(new File(path,"progp"));
	test(new File(path,"paper1"));  // text
	test(new File(path,"paper2"));
	test(new File(path,"book1"));
	test(new File(path,"book2"));
	test(new File(path,"news"));    // unedited news
 	test(new File(path,"bib"));     // formatted bibtex
	test(new File(path,"trans"));   // terminal session
	test(new File(path,"obj1"));    // executable
	test(new File(path,"obj2"));
	test(new File(path,"geo"));     // geophysical data
	test(new File(path,"pic"));     // bitmap
    }

    /** Tests compression/decompression of a given file.
     * @param file File to test.
     * @return <code>true</code> if the test succeeds.
     * @return <code>true</code> if the test succeeds.
     * @throws IOException If there is an underlying I/O exception.
     */ 
    private static  boolean test(File file) throws IOException {
	System.out.println("\nTesting File: " + file);
	FileInputStream in = new FileInputStream(file);
	int available = in.available();
	byte[] bytes = new byte[available];
	in.read(bytes,0,available);
	return testBytes(bytes);
    }

    /** Tests compression/decompression of a given string. String is
     * first rendered as bytes, given current localized default; see
     * {@link java.lang.String#getBytes}.
     * @param text String to test for compression/decompression.
     * @return <code>true</code> if the test succeeds.
     * @throws IOException If there is an underlying I/O exception.
     */
    private static boolean test(String text) throws IOException {
	System.out.println();
	System.out.println("Testing: /" + trim(text) + "/");
	return testBytes(text.getBytes());
    }

    /** Tests compression/decompression of a given sequence of bytes.
     * @param bytes Bytes to test for compression/decompression.
     * @return <code>true</code> if the test succeeds.
     * @throws IOException If there is an underlying I/O exception.
     */
    private static boolean test(byte[] bytes) throws IOException {
	System.out.println();
	System.out.println("Testing byte array with length: " + bytes.length);
	return testBytes(bytes);
     }

    /** Run a test of PPM on the specified bytes using a model of the
     * specified order.
     * @param bytes Bytes to test.
     * @param order Order of PPM model to use.
     * @return <code>true</code> if the test is successful.
     */
    private static boolean testPPMBytes(byte[] bytes, int order) throws IOException {
	return testBytes(bytes, new PPMModel(order), new PPMModel(order), "PPM(" + order + ")" + (order < 10 ? "  ": " "));
    }

    /** Tests given sequence of bytes against various models.
     * @param bytes Bytes to test for compression/decompression.
     * @return <code>true</code> if the test succeeds.
     * @throws IOException If there is an underlying I/O exception.
     */
    private static  boolean testBytes(byte[] bytes) throws IOException {
	boolean pass = true;
	pass = testBytes(bytes, UniformModel.MODEL, UniformModel.MODEL, "Uniform ") && pass;
	pass = testBytes(bytes, new AdaptiveUnigramModel(), new AdaptiveUnigramModel(), "Unigram ") && pass;
	pass = testPPMBytes(bytes,0) && pass;
	pass = testPPMBytes(bytes,1) && pass;
	pass = testPPMBytes(bytes,2) && pass;
	pass = testPPMBytes(bytes,3) && pass;
        pass = testPPMBytes(bytes,4) && pass;
	pass = testPPMBytes(bytes,5) && pass;
	pass = testPPMBytes(bytes,6) && pass;
	pass = testPPMBytes(bytes,7) && pass;
	pass = testPPMBytes(bytes,8) && pass;
	// pass = testPPMBytes(bytes,9) && pass;
	pass = testPPMBytes(bytes,10) && pass;
	pass = testPPMBytes(bytes,12) && pass;
        pass = testPPMBytes(bytes,16) && pass;
        // pass = testPPMBytes(bytes,24) && pass;
        // pass = testPPMBytes(bytes,32) && pass;
	return pass;
    }

    /** Tests specified sequence of bytes with specified models for input and output, and specified name.
     * @param bytes Bytest to test.
     * @param modelIn Model to use for encoding.
     * @param modelOut Model to use for decoding.
     * @param name Name ot use for display.
     * @return <code>true</code> if the test succeeds.
     * @throws IOException If there is an underlying I/O exception.
     */
    private static boolean testBytes(byte[] bytes, 
				     ArithCodeModel modelIn, 
				     ArithCodeModel modelOut, 
				     String name) throws IOException 
    {
	ByteArrayInputStream textBytesIn = new ByteArrayInputStream(bytes);
	ByteArrayOutputStream codeBytesOut = new ByteArrayOutputStream();
	long startTime = System.currentTimeMillis();
	copyStream(textBytesIn,
		   new ArithCodeOutputStream(codeBytesOut, modelIn));
	long encodeTime = elapsed(startTime);
	modelIn = null; // can GC input model
	ByteArrayOutputStream textBytesOut = new ByteArrayOutputStream();
	byte[] codeBytes = codeBytesOut.toByteArray();
	startTime = System.currentTimeMillis();
	copyStream(new ArithCodeInputStream(new ByteArrayInputStream(codeBytes),modelOut),
		   textBytesOut);
	long decodeTime = elapsed(startTime);
	_testSet.record(name,bytes.length,codeBytes.length,encodeTime,decodeTime);
	boolean pass = Arrays.equals(bytes,textBytesOut.toByteArray());
	System.out.print("  " + name + " ");
	System.out.print(intToString(bytes.length,9) + " -> " + intToString(codeBytes.length,9) + " B");
	System.out.print("  "+ compressionRateString(bytes.length,codeBytes.length));
	System.out.print("  enc: " + speedString(bytes.length,encodeTime));
	System.out.print("  dec: " + speedString(bytes.length,decodeTime));
	System.out.println(pass ? "" : "***** FAIL *****");
	return pass;
    }


    /** Returns a string representation of the compression rate indicated by the specified
     * number of original bytes and compressed bytes.  Expressed in bits per byte.
     * @param numOriginalBytes  Number of uncompressed bytes.
     * @param numCompressedBytes Number of bytes in the compressed file.
     * @return String representation of compression rate.
     */
    private static String compressionRateString(int numBytesIn, int numBytesOut) {
	double val = ((double) (int) (1000.0 * (((double) (numBytesOut * 8.0)) / (double) numBytesIn)))/1000.0;
	String result = (val > 1000) ? "?" : (val+ "");
	while (result.length() < 6) result = result + " ";
	return result + "b/B";
    }

    /** Convert an integer to a string, padding with spaces in the front
     * to provide a result of the specified minimum length.
     * @param n Integer to convert to string.
     * @param minLength Minimum length of result.
     * @return String representation of integer, padded to at least specified length.
     */
    private static String intToString(int n, int minLength) {
	String s = Integer.toString(n);
	while (s.length() < minLength) s = ' ' + s;
	return s;
    }

    /** Returns a string representation of the speed of compression indicated by the specified
     * number of original bytes and time in milliseconds.
     * @param numBytes Number of uncompressed bytes.
     * @param numMillis Number of milliseconds.
     * @return String representation of number of bytes per millisecond.
     */
    private static String speedString(int numBytes, long numMillis) {
	int kbS = ((int) ((double)numBytes / (double)numMillis));
	return (kbS > 100000 ? "     ?" : intToString(kbS,6)) + " kB/s";
    }

    /** Truncates string to printable length, appending epenthetic dots if
     * it is truncated.
     * @param in String to truncate.
     * @return Truncated string.
     */
    private static  String trim(String in) {
	return (in.length() <= 32) ? in : (in.substring(0,32) + "...");
    }

    /** Fills the specified byte array with random alphanumeric characters
     * generated by the specified randomizer.
     * @param bs Byte array to fill.
     * @param r Randomizer.
     */
    private static  void nextRandomAlphaNum(byte[] bs, Random r) {
	for (int i = 0; i < bs.length; ++i) {
	    bs[i] = nextRandomAlphaNum(r);
	}
    }

    /** Generates the next random byte between the specified low and
     * high bytes inclusive, using the specified randomizer.
     * @param r Randomizer.
     * @param low Low end of byte range, inclusive.
     * @param high High end of byte range, inclusive.
     * @return Random byte in low to high range.
     */
    private static byte nextByteRange(Random r, int low, int high) {
	return (byte) (low + r.nextInt(1 + high - low));
    }

    /** Returns next random alphabetic or numeric byte as
     * determined by the specified randomizer.
     * @param r Randomizer.
     * @return Next random alpha-numeric byte.
     */
    private static  byte nextRandomAlphaNum(Random r) {
	if (r.nextBoolean()) return nextByteRange(r,(byte)'a',(byte)'z');
	if (r.nextBoolean()) return nextByteRange(r,(byte)'A',(byte)'Z');
	return nextByteRange(r,(byte)'0',(byte)'9');
    }

}

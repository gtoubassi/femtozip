package com.colloquial.arithcode;

/** <P>Interface for an adaptive statistical model of a stream to be used
 * as a basis for arithmetic coding and decoding. As in {@link
 * java.io.InputStream}, bytes are coded as integers in the range
 * <code>0</code> to <code>255</code> and <code>EOF</code> is provided
 * as a constant and coded as <code>-1</code>.  In addition,
 * arithmetic coding requires an integer <code>ESCAPE</code> to code
 * information about the model structure.
 *
 * <P> During encoding, a series of calls will be made to
 * <code>escaped(symbol)</code> where <code>symbol</code> is a byte
 * encoded as an integer in the range 0 to 255 or <code>EOF</code>,
 * and if the result is <code>true</code>, a call to
 * <code>interval(ESCAPE)</code> will be made and the process repeated
 * until a call to <code>escaped(symbol)</code> returns
 * <code>false</code>, at which point a call to
 * <code>interval(symbol)</code> is made and the underlying model is
 * updated.
 *
 * <P> During decoding, a call to <code>total()</code> will be made
 * and then a call to <code>pointToSymbol(count)</code>.  If the
 * result is <code>ESCAPE</code>, the process is repeated.  If the
 * result is a byte encoded as an integer in the range <code>0</code>
 * to <code>255</code> or <code>EOF</code>, the symbol is returned and
 * the underlying model is updated.
 *
 * <P>The probability model required for arithmetic coding is
 * cumulative.  For each outcome, rather than returning a probability,
 * an interval is provided to the coder.  As is usual for arithmetic
 * coding, an interval in <code>[0,1]</code> is represented by three
 * integers, where a low count, a high count, and a total count pick
 * out the interval <code>[low/total,high/total)</code>.
 *
 * <P> For more details, see <a href="../../../tutorial.html">The
 * Arithemtic Coding Tutorial</a>.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see ArithCodeInputStream
 * @see ArithCodeOutputStream
 * @since 1.0
 */
public interface ArithCodeModel {

    /** Returns the total count for the current context.
     * @return Total count for the current context.
     */
    public int totalCount();

    /** Returns the symbol whose interval of low and high counts
     * contains the given count.  Ordinary outcomes are positive
     * integers, and the two special constants <code>EOF</code> or
     * <code>ESCAPE</code>, which are negative.
     * @param count The given count.
     * @return The symbol whose interval contains the given count.
     */
    public int pointToSymbol(int count);

    /** Calculates <code>{low count, high count, total count}</code> for
     * the given symbol in the current context.  The symbol is either
     * an integer representation of a byte (0-255) or -1 to denote end-of-file.
     * The cumulative counts
     * in the return must be such that <code>0 <= low count < high
     * count <= total count</code>.  
     * <P>
     * This method will be called exactly once for each symbol being
     * encoded or decoded, and the calls will be made in the order in
     * which they appear in the original file.  Adaptive models
     * may only update their state to account for seeing a symbol
     * <emph>after</emph> returning its current interval.
     * @param symbol The next symbol to decode.
     * @param result Array into which to write range.
     * @return Array containing low count, high count and total.
     */
    public void interval(int symbol, int[] result);

    /** Returns <code>true</code> if current context has no count
     * interval for given symbol.  Successive calls to
     * <code>escaped(symbol)</code> followed by
     * <code>interval(ESCAPE)</code> must eventually lead to a a
     * <code>false</code> return from <code>escaped(symbol)</code>
     * after a number of calls equal to the maximum context size.
     * The integer representation of symbol is as in <code>interval</code>.
     * @param symbol Symbol to test whether it is encoded.
     * @return <code>true</code> if given symbol is not represented in the current context.
     */
    public boolean escaped(int symbol);

    /** Excludes outcome from occurring in next estimate.  A symbol must
     * not be excluded and then coded or decoded.  Exclusions in the model
     * must be coordinated for encoding and decoding.
     * @param symbol Symbol which can be excluded from the next outcome.
     * @since 1.1
     */
    public void exclude(int symbol);

    /** Increments the model as if it had just encoded or decoded the
     * specified symbol in the stream.  May be used to prime models by
     * "injecting" a symbol into the model's stream without
     * coding/decoding it in the stream of coded bytes.  Calls must be
     * coordinated for encoding and decoding.  Will be called
     * automatically by the models for symbols they encode or decode.
     * @param symbol Symbol to add to the model.  
     * @since 1.1
     */
    public void increment(int symbol); 

    /** Symbol denoting end-of-file.  Guaranteed to be negative. 
     */
    public static final int EOF = -1;

    /** Symbol denoting an escape, meaning that the outcome
     * symbol has no interval in the current context.  Guaranteed to be negative.
     */
    public static final int ESCAPE = -2;

}

package com.colloquial.arithcode;

/** Package utility class for converting integers to bytes and back
 * again in a uniform manner.  Could put this in ByteSet.  
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @since 1.1
 */
final class Converter {

    /** Returns byte coded by the specified integer.
     * @param i Integer to conver to a byte.
     * @return Byte coded by the specified integer.
     */
    static byte integerToByte(int i) {
	return (byte)(i-128);
    }

    /** Returns integer code for the specified byte.
     * @param b Byte to code as an integer.
     * @return Integer code for the specified byte.
     */
    static int byteToInteger(byte b) {
	return 128+ (int)b;
    }

}

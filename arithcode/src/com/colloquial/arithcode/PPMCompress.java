package com.colloquial.arithcode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Command-line function for compressing files or streams.
 * <P>
 * <b>Usage:</b>
 * <BR><code>java PPMCompress [<i>Order</i> [<i>FileIn</i> [<i>FileOut</i>]]]</code>
 * <BR>&nbsp;&nbsp; <code><i>Order</i></code>   : Order of PPM model to use.
 * <BR>&nbsp;&nbsp; <code><i>FileIn</i></code>  : File to compress.  If not specified, uses <code>stdin</code>.
 * <BR>&nbsp;&nbsp; <code><i>FileOut</i></code> : Name of file into which to write the compressed output.  
 * Directory must exist.  If not specified, writes to <code>stdout</code>.
 * <P>
 * For example, <code>java PPMCompress 5 foo foo.ppm</code> uses order 5 compression
 * to compress file <code>foo</code> to file <code>foo.ppm</code>.  <code>proc1 | PPMCompress 8 | proc2</code>
 * takes input from the standard out of <code>proc1</code> and sends the compressed
 * stream to <code>proc2</code>.  
 * 
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see PPMDecompress
 * @since 1.1
 */
public final class PPMCompress {
    
    /** Compress according to the command line specification.  See class documentation for
     * description.
     * @throws IOException If there is an underlying IO exception.
     */
    public static void main(String[] args) throws IOException {
	if (args.length > 3) {
	    System.err.println(USAGE_MESSAGE);
	    System.exit(1);
	}
	Test.copyStream(args.length < 2 ? System.in : new FileInputStream(args[1]),
			new ArithCodeOutputStream(args.length < 3 ? (OutputStream) System.out : new FileOutputStream(args[2]),
						  new PPMModel(args.length < 1 ? 8 : Integer.parseInt(args[0]))));
    }

    /** String detailing usage of class as a main.
     */
    private static String USAGE_MESSAGE = "\n" +
	"  USAGE:\n" +
	"  java PPMCompress [Order [FileIn [FileOut]]]\n" +
	"     Order: Order of PPM model to use.\n" +
	"     FileIn: File to compress.  If not specified, uses stdin\n" +
	"     FileOut: Name of file into which to write the output.\n" + 
	"              Directory must exist.  If not specified, writes to stdout.\n" +
	"\n";
}


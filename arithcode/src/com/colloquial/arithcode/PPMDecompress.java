package com.colloquial.arithcode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Command-line function for decompressing files or streams.
 * <P>
 * <b>Usage:</b>
 * <BR><code>java PPMDecompress [<i>Order</i> [<i>FileIn</i> [<i>FileOut</i>]]]</code>
 * <BR>&nbsp;&nbsp; <code><i>Order</i></code>   : Order of PPM model to use.
 * <BR>&nbsp;&nbsp; <code><i>FileIn</i></code>  : File to decompress.  If not specified, uses stdin
 * <BR>&nbsp;&nbsp; <code><i>FileOut</i></code> : Name of file into which to write the decompressed output.  
 * Directory must exist.  If not specified, writes to <code>stdout</code>.
 * <P>
 * For example, <code>java PPMDeCompress 5 foo.ppm foo</code> uses order 5 compression
 * to decompress file <code>foo.ppm</code> to file <code>foo</code>.  Similarly, <code>proc1 | PPMDecompress 8 | proc2</code>
 * takes input from the standard out of <code>proc1</code> and sends the decompressed
 * stream to <code>proc2</code>.  
 * 
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see PPMCompress
 * @since 1.1
 */
public final class PPMDecompress {

    /** Decompress according to the command line specification.  See class documentation for
     * description.
     * @throws IOException If there is an underlying IO exception.
     */
    public static void main(String[] args) throws IOException {
	if (args.length > 3) {
	    System.err.println(USAGE_MESSAGE);
	    System.exit(1);
	}
	Test.copyStream(new ArithCodeInputStream(args.length < 2 ? System.in : new java.io.FileInputStream(args[1]),
						 new PPMModel(args.length < 1 ? 8 : Integer.parseInt(args[0]))),
			args.length < 3 ? (OutputStream) System.out : new FileOutputStream(args[2])); // cast due to ternary whackiness
    }

    private static String USAGE_MESSAGE = "\n" +
	"  USAGE:\n" +
	"  java PPMDecompress [Order [FileIn [FileOut]]]\n" +
	"     Order: Order of PPM model to use.\n" +
	"     FileIn: File to decompress.  If not specified, uses stdin\n" +
	"     FileOut: Name of file into which to write the decompressed output.\n" +
	"              Directory must exist.  If not specified, writes to stdout.\n" +
	"\n";

}

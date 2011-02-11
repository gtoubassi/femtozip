package com.colloquial.arithcode;

import java.io.IOException;
import java.util.HashMap;

/** Model of a sequence of tokens.  Limited to
 * 256 distinct tokens (to enable coding as PPM).
 */
public final class TokenStreamEncoder {

    public TokenStreamEncoder(ArithEncoder encoder, int tokenSequenceOrder, PPMModel tokenBytesModel) {
	_encoder = encoder;
	_tokenBytesModel = tokenBytesModel;
	_tokenSequenceModel = new PPMModel(tokenSequenceOrder);
	_tokenToSymbolMap = new HashMap();
    }

    public void encode(String token) throws IOException {
	if (_tokenToSymbolMap.containsKey(token)) {
	    encodeToken(((Integer) (_tokenToSymbolMap.get(token))).intValue());
	} else {
	    _tokenToSymbolMap.put(token,new Integer(_nextTokenIndex++));
	    encodeToken(_nextTokenIndex);
	    ++_nextTokenIndex; // must do after encodeToken, because encodeToken uses it
	    encodeBytes(token.getBytes(LATIN1));
	}
    }

    private void encodeToken(int symbol) throws IOException {
	for (int i = _nextTokenIndex+1; i < 256; ++i) _tokenSequenceModel.exclude(i);
	encode(_tokenSequenceModel,symbol);
    }

    private void encodeBytes(byte[] bytes) throws IOException {
	for (int i = 0; i < bytes.length; ++i) {
	    _tokenBytesModel.exclude(LATIN1_UNUSED_BYTES);
	    encode(_tokenBytesModel,Converter.byteToInteger(bytes[i]));
	}
	_tokenBytesModel.exclude(LATIN1_UNUSED_BYTES);
	encode(_tokenBytesModel,0); // uses 0 as separator, which can be trouble if 0 is a valid character
    }

    private void encode(PPMModel model, int symbol) throws IOException {
	// COPIED VERBATIM FROM ArithCodeOutputStream
	while (model.escaped(symbol)) {
	    model.interval(ArithCodeModel.ESCAPE,_interval); // have already done complete walk to compute escape
	    _encoder.encode(_interval);
	}
	model.interval(symbol,_interval); // have already done walk to element to compute escape
	_encoder.encode(_interval); 
    }

    /** Arithmetic encoder used for encoding symbols and the bytes making
     * them up.
     */
    private final ArithEncoder _encoder;

    /** Interval used for coding ranges.
     */
    private final int[] _interval = new int[3];  // ** COPIED VERBATIM FROM ArithCodeOutputStream ***

    /** Index of next token, which must fall between 0 and 255 inclusive.
     */
    private int _nextTokenIndex = 0; 

    /** MOdel for the bytes making up the tokens.
     */
    private final PPMModel _tokenBytesModel; 
    
    /** Model for the sequence of tokens, encoded as bytes, making up
     * the token stream.
     */
    private final PPMModel _tokenSequenceModel;

    /** Maps each token string to an Integer used to encode it.
     */
    private final HashMap _tokenToSymbolMap;

    private final static String LATIN1 = "ISO-8859-1";

    private static final ByteSet LATIN1_UNUSED_BYTES = new ByteSet();
    static {
	for (int i = 1; i <= 8; ++i) LATIN1_UNUSED_BYTES.add(i);
	for (int i = 11; i <= 12; ++i) LATIN1_UNUSED_BYTES.add(i);
	for (int i = 14; i <= 31; ++i) LATIN1_UNUSED_BYTES.add(i);
	for (int i = 127; i <= 159; ++i) LATIN1_UNUSED_BYTES.add(i);
    }

}

package org.toubassi.femtozip.substring;


public class NoopSubstringPacker {

    public NoopSubstringPacker(byte[] dictionary) {
    }
    
    public void pack(byte[] rawBytes, SubstringPacker.Consumer consumer) {
        for (int i = 0, count = rawBytes.length; i < count; i++) {
            consumer.encodeLiteral(((int)rawBytes[i]) & 0xff);
        }
    }
        
}

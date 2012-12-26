package org.toubassi.femtozip.substring;

public class Match {
	final int bestMatchIndex;
    final int bestMatchLength;
    
    public Match(int bestMatchIndex, int bestMatchLength) {
    	this.bestMatchIndex = bestMatchIndex;
    	this.bestMatchLength = bestMatchLength;
    }
}

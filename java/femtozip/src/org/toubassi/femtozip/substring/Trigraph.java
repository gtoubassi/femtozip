/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.toubassi.femtozip.substring;

public final class Trigraph {
    private byte b1, b2, b3;
    
    public Trigraph(byte b1, byte b2, byte b3) {
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }
    
    public boolean equals(Object other) {
        Trigraph o = (Trigraph)other;
        return b1 == o.b1 && b2 == o.b2 && b3 == o.b3;
    }
    
    public int hashCode() {
        // DEFLATE is way smarter, it computes a rolling hash so it doesn't recompute overlapping chars.
        int hash = 0;
        hash = b1;
        hash = b2 + ((hash << 5) - hash);
        hash = b3 + ((hash << 5) - hash);
        return hash;
    }
}

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
package org.toubassi.femtozip.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GZipCompressionModel extends GZipDictionaryCompressionModel {

    public void load(DataInputStream in) throws IOException {
        // Nothing to save.  We override so the base class doesn't save the dictionary
    }

    public void save(DataOutputStream out) throws IOException {
        // Nothing to save.  We override so the base class doesn't save the dictionary
    }

    public void compress(byte[] data, OutputStream out) throws IOException {
        compress(out, null, data); 
    }
}

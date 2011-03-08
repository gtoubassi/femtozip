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
/*
 * FileDocumentList.cpp
 *
 *  Created on: Feb 27, 2011
 *      Author: gtoubassi
 */

#include <iostream>
#include <fstream>

#include "FileDocumentList.h"

using namespace std;

namespace femtozip {

FileDocumentList::FileDocumentList(vector<string>& paths) {
    files = paths;
}

FileDocumentList::~FileDocumentList() {
}

int FileDocumentList::size() {
    return files.size();
}

const char *FileDocumentList::get(int i, int& length) {
    ifstream file(files[i].c_str(), ios::in | ios::binary | ios::ate);
    if (file.is_open()) {
        streampos size = file.tellg();
        char *buf = new char[size];
        file.seekg(0, ios::beg);
        file.read(buf, size);
        file.close();

        length = size;

        return buf;
    } else {
        cout << "Unable to open file " << files[i];
    }
    return 0;
}

}

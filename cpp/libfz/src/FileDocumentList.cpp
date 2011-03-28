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
#include "FileUtil.h"

using namespace std;

namespace femtozip {

FileDocumentList::FileDocumentList(vector<string>& paths, bool preload) : files(paths), preload(preload) {
    if (preload) {
        for (vector<string>::iterator i = paths.begin(); i != paths.end(); i++) {
            data.push_back(new FileData(i->c_str()));
        }
    }
}

FileDocumentList::~FileDocumentList() {
    for (vector<FileData *>::iterator i = data.begin(); i != data.end(); i++) {
        delete[] *i;
    }
}

int FileDocumentList::size() {
    return files.size();
}

const char *FileDocumentList::get(int i, int& length) {
    if (preload) {
        length = data[i]->length;
        return data[i]->data;
    }
    else {
        return FileUtil::readFully(files[i].c_str(), length);
    }
}

void FileDocumentList::release(const char *buf) {
    if (!preload) {
        delete[] buf;
    }
}

}

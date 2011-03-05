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

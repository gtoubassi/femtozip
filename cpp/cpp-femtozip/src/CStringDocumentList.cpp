/*
 * CStringDocumentList.cpp
 *
 *  Created on: Feb 28, 2011
 *      Author: gtoubassi
 */

#include <iostream>
#include <cstdarg>
#include "CStringDocumentList.h"

using namespace std;

namespace femtozip {

CStringDocumentList::CStringDocumentList(vector<const char *> paths) {
    files = paths;
}

CStringDocumentList::CStringDocumentList(const char *path1, ...) {
    files.push_back(path1);

    va_list ap;
    va_start(ap, path1);
    const char *path;
    while ((path = va_arg(ap, const char *)) != 0) {
        files.push_back(path);
    }
    va_end(ap);
}

CStringDocumentList::~CStringDocumentList() {
}

int CStringDocumentList::size() {
    return files.size();
}

const char *CStringDocumentList::get(int i, int& length) {
    const char *path = files[i];
    length = strlen(path);
    char *copy = new char[length + 1];
    return strcpy(copy, path);
}

}

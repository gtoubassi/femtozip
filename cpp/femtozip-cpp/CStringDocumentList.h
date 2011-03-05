/*
 * CStringDocumentList.h
 *
 *  Created on: Feb 28, 2011
 *      Author: gtoubassi
 */

#ifndef CSTRINGDOCUMENTLIST_H_
#define CSTRINGDOCUMENTLIST_H_

#include <vector>
#include "DocumentList.h"

using namespace std;

namespace femtozip {

class CStringDocumentList: public femtozip::DocumentList {

protected:
    vector<const char *> files;

public:
    explicit CStringDocumentList(vector<const char *> paths);
    explicit CStringDocumentList(const char *path1, ...);

    virtual ~CStringDocumentList();

    int size();
    const char *get(int i, int& length);
};

}

#endif /* CSTRINGDOCUMENTLIST_H_ */

/*
 * FileDocumentList.h
 *
 *  Created on: Feb 27, 2011
 *      Author: gtoubassi
 */

#ifndef FILEDOCUMENTLIST_H_
#define FILEDOCUMENTLIST_H_

#include <vector>
#include <string>

#include "DocumentList.h"

using namespace std;

namespace femtozip {

class FileDocumentList: public femtozip::DocumentList {
protected:
	vector<string> files;

public:
	explicit FileDocumentList(vector<string>& paths);
	virtual ~FileDocumentList();

	virtual int size();
	virtual const char *get(int i, int& length);
};

}

#endif /* FILEDOCUMENTLIST_H_ */

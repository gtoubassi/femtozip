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

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
 * DocumentList.h
 *
 *  Created on: Feb 27, 2011
 *      Author: gtoubassi
 */

#ifndef DOCUMENTLIST_H_
#define DOCUMENTLIST_H_

namespace femtozip {

class DocumentList {
public:
	DocumentList();
	virtual ~DocumentList();

	virtual int size() = 0;
	virtual const char *get(int i, int& length) = 0;
	virtual void release(const char *buf) = 0;
};

}

#endif /* DOCUMENTLIST_H_ */

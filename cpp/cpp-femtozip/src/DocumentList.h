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
};

}

#endif /* DOCUMENTLIST_H_ */

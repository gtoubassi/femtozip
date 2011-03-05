/*
 * NoCopyReadOnlyStreamBuf.h
 *
 *  Created on: Mar 4, 2011
 *      Author: gtoubassi
 */

#ifndef NOCOPYREADONLYSTREAMBUF_H_
#define NOCOPYREADONLYSTREAMBUF_H_

#include <streambuf>

using namespace std;

namespace femtozip {


class NoCopyReadOnlyStreamBuf : public streambuf {
public:
    NoCopyReadOnlyStreamBuf(char* buf, int length) {
        setg(buf, buf, buf + length);
    }
};

}

#endif /* NOCOPYREADONLYSTREAMBUF_H_ */

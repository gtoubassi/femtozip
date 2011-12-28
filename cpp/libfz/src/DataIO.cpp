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

#include "DataIO.h"

namespace femtozip {

DataOutput::DataOutput(ostream& out) : out(out) {
}

DataOutput::~DataOutput() {
}

void DataOutput::flush() {
    out.flush();
}

DataOutput& DataOutput::operator<<(bool b) {
    char ch = b ? 1 : 0;
    out.write(&ch, 1);
    return *this;
}

DataOutput& DataOutput::operator<<(int i) {
    int sz = sizeof(i);
    while (sz > 0) {
        char ch = (char)(i & 0xff);
        i >>= 8;
        out.write(&ch, 1);
        sz--;
    }
    return *this;
}

DataOutput& DataOutput::operator<<(long i) {
    int sz = sizeof(i);
    while (sz > 0) {
        char ch = (char)(i & 0xff);
        i >>= 8;
        out.write(&ch, 1);
        sz--;
    }
    return *this;
}

DataOutput& DataOutput::operator<<(short i) {
    int sz = sizeof(i);
    while (sz > 0) {
        char ch = (char)(i & 0xff);
        i >>= 8;
        out.write(&ch, 1);
        sz--;
    }
    return *this;
}

DataOutput& DataOutput::operator<<(const string& str) {
    int len = str.length();
    (*this) << len;
    out.write(str.c_str(), len);
    return *this;
}

void DataOutput::write(const char *buf, int len) {
    out.write(buf, len);
}



DataInput::DataInput(istream& in) : in(in) {
}

DataInput::~DataInput() {
}

DataInput& DataInput::operator>>(bool& b) {
    char ch;
    in.read(&ch, 1);
    b = ch ? true : false;
    return *this;
}

DataInput& DataInput::operator>>(int& i) {
    int sz = sizeof(i);
    i = 0;

    int shift = 0;
    while (in.good() && sz > 0) {
        unsigned char ch;
        in.read((char *)&ch, 1);
        i |= (ch << shift);
        shift += 8;
        sz--;
    }
    return *this;
}

DataInput& DataInput::operator>>(long& i) {
    int sz = sizeof(i);
    i = 0;

    int shift = 0;
    while (in.good() && sz > 0) {
        unsigned char ch;
        in.read((char *)&ch, 1);
        i |= (((long)ch) << shift);
        shift += 8;
        sz--;
    }
    return *this;
}

DataInput& DataInput::operator>>(short& i) {
    int sz = sizeof(i);
    i = 0;

    int shift = 0;
    while (in.good() && sz > 0) {
        unsigned char ch;
        in.read((char *)&ch, 1);
        i |= (ch << shift);
        shift += 8;
        sz--;
    }
    return *this;
}

DataInput& DataInput::operator>>(string& str) {
    int len;
    (*this) >> len;
    if (in.good()) {
        str.resize(len);
        in.read(&str[0], len);
    }
    return *this;
}

void DataInput::read(char *buf, int len) {
    in.read(buf, len);
}

}

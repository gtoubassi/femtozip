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

#ifndef DATAIO_H_
#define DATAIO_H_

#include <iostream>
#include <string>

using namespace std;

namespace femtozip {

class DataOutput {
private:
    ostream& out;

public:
    DataOutput(ostream& out);
    virtual ~DataOutput();

    void flush();

    DataOutput& operator<<(bool i);
    DataOutput& operator<<(int i);
    DataOutput& operator<<(long i);
    DataOutput& operator<<(short i);
    DataOutput& operator<<(const string& str);

    void write(const char *buf, int len);
};

class DataInput {
private:
    istream& in;

public:
    DataInput(istream& in);
    virtual ~DataInput();

    DataInput& operator>>(bool& b);
    DataInput& operator>>(int& i);
    DataInput& operator>>(long& i);
    DataInput& operator>>(short& i);
    DataInput& operator>>(string& str);

    void read(char *buf, int len);
};

}

#endif /* DATAIO_H_ */

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
 * Substring.h
 *
 *  Created on: Mar 1, 2011
 *      Author: gtoubassi
 */

#ifndef SUBSTRING_H_
#define SUBSTRING_H_

namespace femtozip {

class Substring {

private:
    int _index;
    int _length;
    int _score;

    inline static int computeScore(int length, int count) {
        if (length <= 3) {
            return 0;
        }
        return (100 * count * (length - 3)) / length;
    }

public:
    Substring(int index, int length, int count) {
        _index = index;
        _length = length;
        _score = computeScore(length, count);
    }

    ~Substring() {}

    inline int getIndex() const {return _index;};
    inline int getLength() const {return _length;};
    inline int getScore() const {return _score;};

    inline bool operator<(const Substring& other) const {
        return _score < other.getScore();
    }
};

}

#endif /* SUBSTRING_H_ */

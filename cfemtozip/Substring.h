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

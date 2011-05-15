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

#ifndef INTSET_H_
#define INTSET_H_

#include <vector>

using namespace std;

namespace femtozip {

/**
 * Oodles faster for my purposes than hash_set<int>
 */
class IntSet {
private:
    static float load_factor = .7;

    int *buckets;
    size_t capacity;
    size_t max_size;
    size_t s;

    int insert(int n, int *buckets, size_t capacity) {
        size_t index = n % capacity;
        while (buckets[index] != -1) {
            if (buckets[index] == n) {
                return 0;
            }
            index++;
            if (index == capacity) {
                index = 0;
            }
        }

        buckets[index] = n;
        return 1;
    }

    void clear_buckets(int *buckets, size_t capacity) {
        //XXX assumes twos complement.  Must be better way?
        memset(buckets, -1, capacity * sizeof(int));
    }

public:
    IntSet() {
        s = 0;
        capacity = 16;
        buckets = new int[capacity];
        max_size = load_factor * capacity;
        clear_buckets(buckets, capacity);
    }

    virtual ~IntSet() {
        delete[] buckets;
    }

    size_t size() const { return s; }

    void put(int n) {
        if (capacity == max_size) {
            int new_capacity = 2 * capacity;
            int *new_buckets = new int[new_capacity];
            clear_buckets(new_buckets, new_capacity);

            for (int *p = buckets, *end = p + capacity; p != end; p++) {
                if (*p != -1) {
                    insert(*p, new_buckets, new_capacity);
                }
            }

            delete[] buckets;

            buckets = new_buckets;
            capacity = new_capacity;
            max_size = load_factor * capacity;
        }

        s += insert(n, buckets, capacity);
    }

    void clear() {
        if (s != 0) {
            clear_buckets(buckets, capacity);
            s = 0;
        }
    }
};

}

#endif /* INTSET_H_ */

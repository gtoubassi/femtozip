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
 * A fast simple set of positive ints that can't be iterated.
 * This was written because std::set is based on std::tree and
 * is thus slower than it needs to be, and hashmap is non standard
 * and also fairly slow (for unknown reasons).  IntSet was motivated
 * by an optimization round on the dictionary creation process.
 */
class IntSet {
private:
    static constexpr float load_factor = .7;

    int *buckets;
    int *bucket_end;
    size_t capacity;
    size_t max_size;
    size_t set_size;

    inline int insert(int n, int *b, int *end, size_t capacity) {
        int *p = b + (n % capacity);

        while (*p != -1) {
            if (*p == n) {
                return 0;
            }
            p++;
            if (p == end) {
                p = b;
            }
        }

        *p = n;
        return 1;
    }

    void clear_buckets(int *buckets, size_t capacity) {
        //XXX assumes twos complement.  Must be better way?
        memset(buckets, -1, capacity * sizeof(int));
    }

public:
    IntSet(int targetCapacity = 0) {
        set_size = 0;
        capacity = targetCapacity == 0 ? 16 : static_cast<int>(targetCapacity/load_factor);
        buckets = new int[capacity];
        bucket_end = buckets + capacity;
        max_size = static_cast<int>(load_factor * capacity);
        clear_buckets(buckets, capacity);
    }

    virtual ~IntSet() {
        delete[] buckets;
    }

    size_t size() const { return set_size; }

    inline void put(int n) {
        if (set_size >= max_size) {
            int new_capacity = 2 * capacity;
            int *new_buckets = new int[new_capacity];
            int *new_bucket_end = new_buckets + new_capacity;
            clear_buckets(new_buckets, new_capacity);

            for (int *p = buckets; p != bucket_end; p++) {
                if (*p != -1) {
                    insert(*p, new_buckets, new_bucket_end, new_capacity);
                }
            }

            delete[] buckets;

            buckets = new_buckets;
            capacity = new_capacity;
            bucket_end = new_bucket_end;
            max_size = static_cast<int>(load_factor * capacity);
        }

        set_size += insert(n, buckets, bucket_end, capacity);
    }

    void clear() {
        if (set_size != 0) {
            clear_buckets(buckets, capacity);
            set_size = 0;
        }
    }
};

}

#endif /* INTSET_H_ */

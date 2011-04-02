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

#include <iostream>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <string.h>
#include "DataIO.h"
#include "DocumentList.h"
#include "CompressionModel.h"
#include "femtozip.h"

using namespace std;
using namespace femtozip;

class CCallbackDocumentList : public DocumentList {
private:
    int count;
    const char *(*get_callback)(int doc_index, int *doc_len, void *user_data);
    void (*release_callback)(const char *buf, void *user_data);
    void *user_data;

public:
    CCallbackDocumentList(int size,  const char *(*get_callback)(int doc_index, int *doc_len, void *user_data), void (*release_callback)(const char *buf, void *user_data), void *callback_user_data) : count(size), get_callback(get_callback), release_callback(release_callback), user_data(callback_user_data) {};

    virtual ~CCallbackDocumentList() {}

    virtual int size() {
        return count;
    }

    virtual const char *get(int i, int& length) {
        return get_callback(i, &length, user_data);
    }

    virtual void release(const char *buf) {
        release_callback(buf, user_data);
    }
};


#ifdef __cplusplus
extern "C" {
#endif


void *fz_load_model(const char *path) {
    ifstream file(path, ios::in | ios::binary);
    DataInput in(file);
    CompressionModel *model = CompressionModel::loadModel(in);
    file.close();
    return reinterpret_cast<void *>(model);
}

int fz_save_model(void *model, const char *path) {
    CompressionModel *m = reinterpret_cast<CompressionModel*>(model);
    ofstream file(path, ios::out | ios::binary | ios::trunc);
    DataOutput out(file);
    CompressionModel::saveModel(*m, out);
    out.flush();
    file.close();
    return file.fail() ? 1 : 0;
}

void *fz_build_model(int num_docs, const char *(*get_callback)(int doc_index, int *doc_len, void *user_data), void (*release_callback)(const char *buf, void *user_data), void *callback_user_data) {
    CCallbackDocumentList documents(num_docs, get_callback, release_callback, callback_user_data);
    CompressionModel *model = CompressionModel::buildOptimalModel(documents);
    return reinterpret_cast<void *>(model);
}

void fz_release_model(void *model) {
    CompressionModel *m = reinterpret_cast<CompressionModel*>(model);
    delete m;
}

int fz_compress(void *model, const char *source, int source_len, char *dest, int dest_capacity) {
    CompressionModel *m = reinterpret_cast<CompressionModel*>(model);
    ostringstream out;
    m->compress(source, source_len, out);
    string outstr = out.str();
    if (outstr.length() > (size_t)dest_capacity) {
        return -outstr.length();
    }
    memcpy(dest, outstr.c_str(), outstr.length());
    return outstr.length();
}

int fz_decompress(void *model, const char *source, int source_len, char *dest, int dest_capacity) {
    CompressionModel *m = reinterpret_cast<CompressionModel*>(model);
    ostringstream out;
    m->decompress(source, source_len, out);
    string outstr = out.str();
    if (outstr.length() > (size_t)dest_capacity) {
        return -outstr.length();
    }
    memcpy(dest, outstr.c_str(), outstr.length());
    return outstr.length();
}


#ifdef __cplusplus
}
#endif

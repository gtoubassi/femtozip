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

/**
 * A simple example usage of the FemtoZip C API (without the load and save).
 * For more see femtozip.h
 */

#include <stdlib.h>
#include <strings.h>
#include <femtozip.h>

const char *example_get_doc_callback(int doc_index, int *doc_len, void *user_data) {
    char **docs = (char **)(user_data);
    *doc_len = strlen(docs[doc_index]);
    return docs[doc_index];
}

void example_release_doc_callback(const char *buf, void *user_data) {
}

int exampleCApi() {
    const char *train_docs[] = {"http://espn.de", "http://popsugar.de",
            "http://google.de", "http://yahoo.de",
            "http://www.linkedin.com", "http://www.facebook.com",
            "http:www.stanford.edu"};
    int num_docs = sizeof(train_docs) / sizeof(train_docs[0]);

    void *model = fz_build_model(num_docs, example_get_doc_callback, example_release_doc_callback, train_docs);

    char compressed[1024];
    const char *test_doc = "check out http://www.facebook.com/someone";
    int compressed_len = fz_compress(model, test_doc, strlen(test_doc), compressed, sizeof(compressed));
    if (compressed_len < 0) {
        return 1;
    }

    char decompressed[1024];
    int decompressed_len = fz_decompress(model, compressed, compressed_len, decompressed, sizeof(decompressed));
    if (decompressed_len < 0 ||
        (unsigned int)decompressed_len != strlen(test_doc) ||
        memcmp(decompressed, test_doc, decompressed_len) != 0)
    {
        return 2;
    }

    fz_release_model(model);
    return 0;
}


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


#ifndef FEMTOZIP_H_
#define FEMTOZIP_H_

#ifdef __cplusplus
extern "C" {
#endif


/**
 * Returns NULL if the load failed
 */
void *fz_load_model(const char *path);

/**
 * Returns 0 on success
 */
int fz_save_model(void *model, const char *path);

/**
 * Returns NULL on failure
 */
void *fz_build_model(int num_docs, const char *(*get_callback)(int doc_index, int *doc_len), void (*release_callback)(const char *buf));

/**
 * Frees memory associated with the model.  The model is invalid after this point.
 * All calls to fz_load_model or fz_build_model should be paired with a call to
 * fz_release_model.
 */
void fz_release_model(void *model);

/*
 * Returns actual length of the compressed data written to dest if successful.  General
 * failure returns 0, and negative number means the operation required a bigger buffer
 * of abs(return value).  Try again with an appropriately sized buffer.
 */
int fz_compress(void *model, const char *source, int source_len, char *dest, int dest_capacity);

/*
 * Returns actual length of the decompressed data written to dest if successful.  General
 * failure returns 0, and negative number means the operation required a bigger buffer
 * of abs(return value).  Try again with an appropriately sized buffer.
 */
int fz_decompress(void *model, const char *source, int source_len, char *dest, int dest_capacity);


#ifdef __cplusplus
}
#endif


#endif /* FEMTOZIP_H_ */

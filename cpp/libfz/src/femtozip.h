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
 * A simple C API to the FemtoZip compressor and model building functionality.
 * The basic recipe for using femtozip is to:
 *
 * 1. Collect sample "documents" (document is simply a byte buffer) which
 *    can be used to build a model.
 * 2. Call fz_build_model with callbacks which can be used by the model
 *    builder to retrieve the documents as needed.
 * 3. Call fz_save_model with the new model.
 * 4. Release the model via fz_release_model.
 * 5. Later (perhaps in a different process), load the model via fz_load_model.
 * 6. compress/decompress documents via fz_compress/fz_decompress
 * 7. fz_release_model the model when it is no longer needed.
 *
 * For a simple C example, see example.c in the source distribution of FemtoZip
 * at http://github.com/gtoubassi/femtozip
 */

#ifndef FEMTOZIP_H_
#define FEMTOZIP_H_

#ifdef __cplusplus
extern "C" {
#endif


/**
 * Loads a model from the file 'path'.  The model should
 * be released with fz_release_model when it is no longer
 * needed.  The return value is a model used in subsequent
 * calls to fz_compress and fz_decompress.  Returns NULL
 * if the load failed.
 *
 * @see fz_save_model
 */
void *fz_load_model(const char *path);

/**
 * Saves the specified 'model' (returned from fz_build_model)
 * to the specified 'path'.  Returns 0 on success, non zero
 * if an error occurred.
 *
 * @see fz_load_model
 */
int fz_save_model(void *model, const char *path);

/**
 * Builds a model based on the sample documents provided by the callbacks.
 * 'num_docs' represents the number of documents that can be retrieved from
 * the callbacks.  'get_callback' is invoked repeatedly with doc_index
 * ranging from [0, num_docs).  It is expected to return the document as
 * well as its length in bytes via 'doc_len'.  user_data will simply be the
 * 'callback_user_data' specified to fz_build_model as argument 4.  When
 * the model building process is done with the document, it will call
 * 'release_callback' with the specified document and the associated user_data.
 * After a model is built, it should most likely be saved via fz_save_model,
 * and eventually released via fz_release_model.  Returns NULL on failure.
 *
 * @see fz_save_model
 * @see fz_release_model
 */
void *fz_build_model(int num_docs, const char *(*get_callback)(int doc_index, int *doc_len, void *user_data), void (*release_callback)(const char *buf, void *user_data), void *callback_user_data);

/**
 * Frees memory associated with the 'model'.  The model is invalid after this point.
 * All calls to fz_load_model or fz_build_model should be paired with a call to
 * fz_release_model.
 *
 * @see fz_load_model
 * @see fz_build_model
 */
void fz_release_model(void *model);

/*
 * Attempts to compress 'source_len' bytes starting at 'source' into 'dest'
 * using the specified 'model'.  'dest_capacity' represents the maximum size of
 * the 'dest' buffer.  If the compressed data fits, its length is returned.  If
 * the dest buffer was not big enough, -(required size) is returned.  Meaning if
 * 200 bytes were needed but 'dest_capacity' was < 200, then -200 would be returned.
 * In that case try again with a larger buffer.  Calling fz_decompress with the
 * resulting buffer will return the original bytes.
 *
 * @see fz_decompress.
 */
int fz_compress(void *model, const char *source, int source_len, char *dest, int dest_capacity);

/*
 * Attempts to dcompress 'source_len' bytes starting at 'source' into 'dest'
 * using the specified 'model'.  'dest_capacity' represents the maximum size of
 * the 'dest' buffer.  If the decompressed data fits, its length is returned.  If
 * the dest buffer was not big enough, -(required size) is returned.  Meaning if
 * 200 bytes were needed but 'dest_capacity' was < 200, then -200 would be returned.
 * In that case try again with a larger buffer.
 *
 * @see fz_compress.
 */
int fz_decompress(void *model, const char *source, int source_len, char *dest, int dest_capacity);


#ifdef __cplusplus
}
#endif


#endif /* FEMTOZIP_H_ */

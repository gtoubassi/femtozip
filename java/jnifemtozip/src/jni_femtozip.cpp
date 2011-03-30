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

#include <alloca.h>
#include <femtozip.h>
#include <jni.h>
#include <string.h>
#include <iostream>

#ifdef __cplusplus
extern "C" {
#endif

using namespace std;


void *getModel(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID modelFieldId;
    modelFieldId = env->GetFieldID(cls, "nativeModel", "J");
    if (!modelFieldId) {
        return 0; // Should throw
    }

    return (void *)(env->GetLongField(obj, modelFieldId));
}

void setModel(JNIEnv *env, jobject obj, void *model) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID modelFieldId;
    modelFieldId = env->GetFieldID(cls, "nativeModel", "J");
    if (!modelFieldId) {
        return; // Should throw
    }

    env->SetLongField(obj, modelFieldId, (jlong)model);
}

/*
 * Class:     org_toubassi_femtozip_models_NativeCompressionModel
 * Method:    load
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_toubassi_femtozip_models_NativeCompressionModel_load(JNIEnv *env, jobject obj, jstring path) {
    const char *pathCStr = env->GetStringUTFChars(path, NULL);
    if (!pathCStr) {
        return; /* OutOfMemoryError already pending */
    }

    void *model = fz_load_model(pathCStr);

    env->ReleaseStringUTFChars(path, pathCStr);

    setModel(env, obj, model);
}

JNIEXPORT void JNICALL Java_org_toubassi_femtozip_models_NativeCompressionModel_save(JNIEnv *env, jobject obj, jstring path) {
    void *model = getModel(env, obj);
    if (!model) {
        return;
    }

    const char *pathCStr = env->GetStringUTFChars(path, NULL);
    if (!pathCStr) {
        return; /* OutOfMemoryError already pending */
    }

    fz_save_model(model, pathCStr);

    env->ReleaseStringUTFChars(path, pathCStr);
}

const char *get_doc_callback(int doc_index, int *doc_len, void *user_data) {
    void **data = reinterpret_cast<void **>(user_data);
    JNIEnv *env = reinterpret_cast<JNIEnv *>(data[0]);
    jobject docList = reinterpret_cast<jobject>(data[1]);

    jclass cls = env->GetObjectClass(docList);
    jmethodID getMethodId = env->GetMethodID(cls, "get", "(I)[B");
    if (!getMethodId) {
        return 0;
    }

    jbyteArray byteArray = (jbyteArray)env->CallObjectMethod(docList, getMethodId, doc_index); //XXX Is the raw cast to jbyteArray kosher?
    *doc_len = env->GetArrayLength(byteArray);

    jbyte *source = env->GetByteArrayElements(byteArray, NULL);

    char *buf = new char[*doc_len];
    // Bummer we have to copy, but its the easiest way to deal with cleanup in release, and
    // document iteration during model building isn't performance critical.
    memcpy(buf, source, *doc_len);
    return buf;
}

void release_doc_callback(const char *buf, void *user_data) {
    delete[] buf;
}

JNIEXPORT void JNICALL Java_org_toubassi_femtozip_models_NativeCompressionModel_build(JNIEnv *env, jobject obj, jobject docList) {

    void *model = getModel(env, obj);
    if (model) {
        fz_release_model(model);
        setModel(env, obj, 0);
    }

    jclass cls = env->GetObjectClass(docList);
    jmethodID sizeMethodId = env->GetMethodID(cls, "size", "()I");
    if (!sizeMethodId) {
        return;
    }

    int numDocs = env->CallIntMethod(docList, sizeMethodId);

    void *data[2] = {env, docList};
    model = fz_build_model(numDocs, get_doc_callback, release_doc_callback, data);
    setModel(env, obj, model);
}

JNIEXPORT jint JNICALL Java_org_toubassi_femtozip_models_NativeCompressionModel_compress(JNIEnv *env, jobject obj, jbyteArray sourceBytesArray, jbyteArray destBytesArray) {
    void *model = getModel(env, obj);
    if (!model) {
        return 0;
    }

    jbyte *source;
    source = env->GetByteArrayElements(sourceBytesArray, NULL);
    int sourceLength = env->GetArrayLength(sourceBytesArray);

    int destCapacity = env->GetArrayLength(destBytesArray);
    jbyte *dest = (jbyte *)alloca(destCapacity);

    int length = fz_compress(model, (const char *)source, sourceLength, (char *)dest, destCapacity);

    env->ReleaseByteArrayElements(sourceBytesArray, source, 0);

    if (length < 0) {
        return length;
    }

    env->SetByteArrayRegion(destBytesArray, 0, length, dest);

    return length;
}

JNIEXPORT jint JNICALL Java_org_toubassi_femtozip_models_NativeCompressionModel_decompress(JNIEnv *env, jobject obj, jbyteArray sourceBytesArray, jbyteArray destBytesArray) {
    void *model = getModel(env, obj);
    if (!model) {
        return 0;
    }

    jbyte *source;
    source = env->GetByteArrayElements(sourceBytesArray, NULL);
    int sourceLength = env->GetArrayLength(sourceBytesArray);

    int destCapacity = env->GetArrayLength(destBytesArray);
    jbyte *dest = (jbyte *)alloca(destCapacity);

    int length = fz_decompress(model, (const char *)source, sourceLength, (char *)dest, destCapacity);

    env->ReleaseByteArrayElements(sourceBytesArray, source, 0);

    if (length < 0) {
        return length;
    }

    env->SetByteArrayRegion(destBytesArray, 0, length, dest);

    return length;
}

JNIEXPORT void JNICALL Java_org_toubassi_femtozip_models_NativeCompressionModel_free(JNIEnv *env, jobject obj) {
    void *model = getModel(env, obj);
    if (!model) {
        return;
    }

    fz_release_model(model);
}


#ifdef __cplusplus
}
#endif




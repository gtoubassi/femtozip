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
//============================================================================

#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <pthread.h>
#include <sys/time.h>
#include <string.h>
#include <stdlib.h>
#include <FileDocumentList.h>
#include <CStringDocumentList.h>
#include <DictionaryOptimizer.h>
#include <SubstringPacker.h>
#include <SubstringUnpacker.h>
#include <VerboseStringConsumer.h>
#include <BitInput.h>
#include <BitOutput.h>
#include <FrequencyHuffmanModel.h>
#include <HuffmanDecoder.h>
#include <HuffmanEncoder.h>
#include <CompressionModel.h>
#include <PureHuffmanCompressionModel.h>
#include <FemtoZipCompressionModel.h>
#include <GZipCompressionModel.h>
#include <GZipDictionaryCompressionModel.h>
#include <DataIO.h>
#include <femtozip.h>
#include <IntSet.h>

using namespace std;
using namespace femtozip;

static int failureCount = 0;
static int successCount = 0;

static string PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
static string PanamaString = "a man a plan a canal panama";
static string PreambleDictionary = " of and for the a United States ";

void assertTrue(bool result, const string& message) {
    if (!result) {
        cout << "Test Failure: " << message << endl;
        failureCount++;
    }
    else {
        successCount++;
    }
}

long long getTimeMillis() {
    timeval tim;
    gettimeofday(&tim, NULL);
    return tim.tv_sec * 1000 + tim.tv_usec / 1000;
}

void reportTestResults() {
    cout << failureCount << " failures" << endl;
    cout << successCount << " successes" << endl;
}

void testDictionaryOptimizerPack() {
    {
        CStringDocumentList docs(PreambleString.c_str(), NULL);
        DictionaryOptimizer optimizer(docs);
        string dict = optimizer.optimize(64*1024);
        assertTrue(dict == "ce, in to sticure our, pros of and ity, e the for the establish the United States", string("Dict for US preamble wrong") + dict);
    }

    {
        CStringDocumentList docs2(PanamaString.c_str(), NULL);
        DictionaryOptimizer optimizer2(docs2);
        string dict = optimizer2.optimize(64*1024);
        assertTrue(dict == "an a ", string("Dict for panama wrong") + dict);
    }

    {
        CStringDocumentList docs("11111", "11111", "00000", NULL);
        DictionaryOptimizer optimizer(docs);
        string dict = optimizer.optimize(64*1024);
        assertTrue(dict == "000011111", string("wrong dict: ") + dict);
    }
}

void testDocumentList() {
    CStringDocumentList docs("hello", "there", "how are you", NULL);

    assertTrue(docs.size() == 3, "Wrong doc count for doclist expected 3, was");
    int length;
    const char *buf = docs.get(0, length);
    assertTrue(length == 5, "Wrong length for doc 0");
    assertTrue(memcmp("hello", buf, length) == 0, "Wrong bytes for doc 0");
    docs.release(buf);

    buf = docs.get(1, length);
    assertTrue(length == 5, "Wrong length for doc 1");
    assertTrue(memcmp("there", buf, length) == 0, "Wrong bytes for doc 1");
    docs.release(buf);

    buf = docs.get(2, length);
    assertTrue(length == 11, "Wrong length for doc 2");
    assertTrue(memcmp("how are you", buf, length) == 0, "Wrong bytes for doc 2");
    docs.release(buf);
}

void testPack(const char *expected, const char *buf, const char *dict = "") {
    {
        SubstringPacker packer(dict ? dict : "", dict ? strlen(dict) : 0);
        VerboseStringConsumer consumer;

        packer.pack(buf, strlen(buf), consumer, 0);
        string out = consumer.getOutput();

        assertTrue(out == expected, "Failure: got '" + out + "' expected '" + expected + "'");

        vector<char> unpackedBytes;
        SubstringUnpacker unpacker(dict ? dict : "", dict ? strlen(dict) : 0, unpackedBytes);
        packer.pack(buf, strlen(buf), unpacker, 0);
        assertTrue(strlen(buf) == unpackedBytes.size(), "Unpacked bytes have different length");
        if (unpackedBytes.size() > 0) {
            assertTrue(strncmp(buf, &unpackedBytes[0], strlen(buf)) == 0, "Could not roundtrip packed string");
        }
    }
}

void testSubstringPacker() {
    // Simple cases

    testPack("", "");
    testPack("garrick <-8,7>", "garrick garrick");
    testPack("garrick <-8,15>", "garrick garrick garrick");
    testPack("garrick <-8,15>x", "garrick garrick garrickx");
    testPack("garrick <-8,15>xx", "garrick garrick garrickxx");
    testPack("garrick <-8,15>xxx", "garrick garrick garrickxxx");
    testPack("garrick toubassi <-17,24>", "garrick toubassi garrick toubassi garrick");
    testPack("garrick toubassi <-17,17>x<-19,8>", "garrick toubassi garrick toubassi x garrick");
    testPack("garrick toubassi <-17,8><-25,16>", "garrick toubassi garrick garrick toubassi");

    // With a dictionary
    testPack("<-7,7> toubassi", "garrick toubassi", "garrick");
    testPack("garrick <-16,8>", "garrick toubassi", "toubassi");
    testPack("<-7,7> <-24,8>", "garrick toubassi", "toubassi garrick");
    testPack("a<-1,6>", "aaaaaaa", "aaaa");

    // Run length encoding
    testPack("a", "a");
    testPack("aa", "aa");
    testPack("aaa", "aaa");
    testPack("a<-1,4>", "aaaaa");
    testPack("a <-2,8>", "a a a a a ");
    testPack("a <-2,7>", "a a a a a");
    testPack("a <-2,7>x", "a a a a ax");

    // Test next match better than previous
    testPack("arrickgarg<-10,6>", "arrickgargarrick");

    // Test multiple matches in a payload
    testPack("garrick <-8,8>nadim<-6,7>toubassi<-9,9>", "garrick garrick nadim nadim toubassi toubassi");
}

void testPreferNearerMatches() {
    // Simple, no dict
    testPack("the <-4,4>x<-6,4>", "the the x the");

    // Have a match both in the dict and local, and prefer the local one
    // because we match dicts and local separately (since dict is prehashed)
    // this was actually a bug at one point.
    testPack("<-7,7> <-8,7>", "garrick garrick", "garrick");

}

void testBitIO() {
    // Test empty file eof
    {
        BitInput bitIn("", 0);
        int bit = bitIn.readBit();
        assertTrue(bit == -1, "Didn't get eof from empty bit stream");
    }

    // Write 1 bit, read 1 bit
    {
        vector<char> out;
        BitOutput bitOut(out);
        bitOut.writeBit(1);
        bitOut.flush();

        BitInput bitIn(&out[0], out.size());
        assertTrue(bitIn.readBit() == 1, "Expected 1 from bit input");
        for (int i = 0; i < 7; i++) {
            assertTrue(bitIn.readBit() == 0, "Expected 0 from bit input");
        }
        assertTrue(bitIn.readBit() == -1, "Expected eof from bit input");
        // Make sure it keeps returning -1
        assertTrue(bitIn.readBit() == -1, "Expected eof from bit input");
    }

    // Write N bits (from 1 to 256), make sure we get eof after
    for (int j = 1; j <= 256; j++) {
        vector<char> out;
        BitOutput bitOut(out);
        for (int i = 0; i < j; i++) {
            bitOut.writeBit(i & 1);
        }
        bitOut.flush();

        BitInput bitIn(&out[0], out.size());
        for (int i = 0; i < j; i++) {
            int bit = bitIn.readBit();
            assertTrue(bit == (i & 1), string("Got wrong bit ") + (bit ? "1" : "0"));
        }
        if (j % 8) {
            for (int i = 0; i < 8 - (j % 8); i++) {
                int bit = bitIn.readBit();
                assertTrue(bit == 0, string("Got wrong bit ") + (bit ? "1" : "0"));
            }
        }
        assertTrue(bitIn.readBit() == -1, "Expected eof from bit input");
    }

    // Write 8 bits all 1s
    {
        vector<char> out;
        BitOutput bitOut(out);
        for (int i = 0; i < 8; i++) {
            bitOut.writeBit(1);
        }
        bitOut.flush();

        BitInput bitIn(&out[0], out.size());
        for (int i = 0; i < 8; i++) {
            int bit = bitIn.readBit();
            assertTrue(bit == 1, string("Got wrong bit ") + (bit ? "1" : "0"));
        }
        assertTrue(bitIn.readBit() == -1, "Expected eof from bit input");
    }
}

template <class T> void huffmanEncodeStringWithModel(vector<int>& data, T& model) {
    vector<char> out;
    HuffmanEncoder<T> encoder(out, model);

    for (vector<int>::iterator i = data.begin(); i != data.end(); i++) {
        encoder.encodeSymbol(*i);
    }
    encoder.finish();

    HuffmanDecoder<T> decoder(&out[0], out.size(), model);
    vector<int> decompressed;
    int symbol;
    while ((symbol = decoder.decodeSymbol()) != -1) {
        decompressed.push_back(symbol);
    }

    assertTrue(data.size() == decompressed.size(), "Wrong length for huffman decoded string");

    for (int i = 0, count = data.size(); i < count; i++) {
        assertTrue(decompressed[i] == data[i], "Wrong symbol huffman decoded");
    }
}

void huffmanEncodeString(const char *string, bool allSymbolsSampled) {
    vector<int> data(strlen(string));
    for (int i = 0, count = strlen(string); i < count; i++) {
        data[i] = ((int)string[i]) & 0xff;
    }
    vector<int> histogram;
    FrequencyHuffmanModel::computeHistogramWithEOFSymbol(histogram, string, strlen(string));
    FrequencyHuffmanModel model(histogram, allSymbolsSampled);

    huffmanEncodeStringWithModel<FrequencyHuffmanModel>(data, model);
}

void testHuffman() {
    // Something simple
    {
        huffmanEncodeString("a man a plan a canal panama", true);
        huffmanEncodeString("a man a plan a canal panama", false);
    }

    // Test nested DecodingTables
    {
        srand(1234567);
        for (int dataSize = 2; dataSize < 500; dataSize++) {
            vector<int> histogram(dataSize);
            for (int i = 0, count = histogram.size(); i < count; i++) {
                histogram[i] = 20 + (rand() % 10);
            }

            FrequencyHuffmanModel model(histogram, false);

            vector<int> data(histogram.size());
            for (int i = 0, count = data.size(); i < count; i++) {
                data[i] = rand() % (histogram.size() - 1); // -1 so we don't emit EOF mid stream!
            }

            huffmanEncodeStringWithModel<FrequencyHuffmanModel>(data, model);
        }
    }
}

void testModel(const char *source, const char *dictionary, CompressionModel& model, CompressionModel& modelReload, size_t expectedSize) {
    if (!dictionary) {
        dictionary = "";
    }

    model.setDictionary(dictionary, strlen(dictionary));
    CStringDocumentList documents(source, NULL);
    model.build(documents);

    ostringstream out;
    model.compress(source, strlen(source), out);
    string compressed = out.str();

    assertTrue(compressed.length() == expectedSize, string("Wrong compressed size for ") + model.typeName());

    ostringstream modelOut;
    DataOutput dataOut(modelOut);
    model.save(dataOut);
    dataOut.flush();
    istringstream modelIn(modelOut.str());
    DataInput dataIn(modelIn);
    modelReload.load(dataIn);

    ostringstream out2;
    modelReload.decompress(compressed.c_str(), compressed.length(), out2);
    string decompressed = out2.str();

    assertTrue(decompressed == source, string("Mismatched string got: '") + decompressed + "' expected '" + source + "' for " + modelReload.typeName());
}

void testCompressionModels() {
    PureHuffmanCompressionModel pureHuffman, pureHuffman1;
    testModel(PreambleString.c_str(), PreambleDictionary.c_str(), pureHuffman, pureHuffman1, 211);
    FemtoZipCompressionModel offsetNibble, offsetNibble1;
    testModel(PreambleString.c_str(), PreambleDictionary.c_str(), offsetNibble, offsetNibble1, 205);
    GZipCompressionModel gzipModel, gzipModel1;
    testModel(PreambleString.c_str(), PreambleDictionary.c_str(), gzipModel, gzipModel1, 210);
    GZipDictionaryCompressionModel gzipDictModel, gzipDictModel1;
    testModel(PreambleString.c_str(), PreambleDictionary.c_str(), gzipDictModel, gzipDictModel1, 204);
}

void testDocumentUniquenessScoring() {
    FemtoZipCompressionModel model;
    CStringDocumentList docs("garrick1garrick2garrick3garrick4garrick", "xtoubassigarrick", "ytoubassi", "ztoubassi", NULL);

    model.build(docs);

    int length;
    const char *dict = model.getDictionary(length);
    assertTrue(strncmp(dict, "garricktoubassi", length) == 0, "Got wrong dict back in testDocumentUniquenessScoring");
}

void testNonexistantStrings() {
    FemtoZipCompressionModel model;
    CStringDocumentList docs("http://espn.de", "http://popsugar.de", "http://google.de", "http://yahoo.de", "gtoubassi", "gtoubassi", NULL);
    model.build(docs);

    int length;
    const char *dict = model.getDictionary(length);
    // Make sure it doesn't think .dehttp:// is a good one
    assertTrue(strncmp(dict, "gtoubassihttp://", length), "Got wrong dict back in testDocumentUniquenessScoring");
}


void testModelOptimization() {
    {
        CStringDocumentList docs("20161","14219","29477","53380","10626","64782","32972","9313",NULL);
        CompressionModel *model = CompressionModel::buildOptimalModel(docs, true);

        const char *buf = "330539321547621098083609223674246055";
        ostringstream out;
        model->compress(buf, strlen(buf), out);
        string compressed = out.str();

        ostringstream out2;
        model->decompress(compressed.c_str(), compressed.length(), out2);
        string decompressed = out2.str();
        assertTrue(decompressed == buf, string("Mismatched string got: '") + decompressed + "' expected '" + buf);
        assertTrue(strcmp("PureHuffman", model->typeName()) == 0, "Expected PureHuffman for binary data");

        delete model;
    }
    {
        CStringDocumentList docs("http://en.wikipedia.org", "http://www.yahoo.com", "http://mail.google.com", NULL);
        CompressionModel *model = CompressionModel::buildOptimalModel(docs, true);

        const char *buf = "http://www.popsugar.com/?page=1";
        ostringstream out;
        model->compress(buf, strlen(buf), out);
        string compressed = out.str();

        ostringstream out2;
        model->decompress(compressed.c_str(), compressed.length(), out2);
        string decompressed = out2.str();
        assertTrue(decompressed == buf, string("Mismatched string got: '") + decompressed + "' expected '" + buf);
        assertTrue(strcmp("FemtoZip", model->typeName()) == 0, "Expected FemtoZip for text data");
        delete model;
    }
}

void testGZipModel() {
    GZipCompressionModel model;
    ostringstream out;
    const char *buf = "1111111111111111111111111111111112111111";
    model.compress(buf, strlen(buf), out);
    string compressed = out.str();

    ostringstream out2;
    model.decompress(compressed.c_str(), compressed.length(), out2);
    string decompressed = out2.str();

    assertTrue(decompressed == buf, string("GZ round trip failed") + decompressed);
}

void testDataIO() {

    ostringstream outstr;
    DataOutput out(outstr);

    out << false << true << 0 << 1 << -1 << 2000000000 << -2000000001 << 100000000000LL << -100000000000LL << ((short)20000) << ((short)-20001) << string("San Francisco");
    out.write("12345", 5);
    out.flush();

    istringstream instr(outstr.str());
    DataInput in(instr);

    bool b;
    short s;
    int i;
    long long l;
    string str;
    char buf[12];

    in >> b;
    assertTrue(b == false, "Expected false");
    in >> b;
    assertTrue(b == true, "Expected true");
    in >> i;
    assertTrue(i == 0, "Expected 0");
    in >> i;
    assertTrue(i == 1, "Expected 1");
    in >> i;
    assertTrue(i == -1, "Expected -1");
    in >> i;
    assertTrue(i == 2000000000, "Expected 2000000000");
    in >> i;
    assertTrue(i == -2000000001, "Expected -2000000001");
    in >> l;
    assertTrue(l == 100000000000LL, "Expected 100000000000L");
    in >> l;
    assertTrue(l == -100000000000LL, "Expected -100000000000L");
    in >> s;
    assertTrue(s == (short)20000, "Expected 20000");
    in >> s;
    assertTrue(s == (short)-20001, "Expected -20001");
    in >> str;
    assertTrue(str == "San Francisco", "Expected San Francisco");
    in.read(buf, 5);
    assertTrue(strncmp(buf, "12345", 5) == 0, "Expected 12345");
}

void *runThread(void *data) {
    CompressionModel *model = reinterpret_cast<CompressionModel *>(data);
    string source;
    for (int i = 0, count = 256 + (rand() % 64); i < count; i++) {
        source.push_back('a' + (rand() % 26));
    }

    long long start = getTimeMillis();

    while (getTimeMillis() - start < 1500) {
        ostringstream ostr;
        model->compress(&source[0], source.size(), ostr);
        string outstr = ostr.str();
        ostringstream ostr2;
        model->decompress(outstr.c_str(), outstr.length(), ostr2);
        string decompressed = ostr2.str();

        assertTrue(decompressed == source, string("Failed to verify multi threaded data for ") + model->typeName());
    }
    return 0;
}

void testThreadedCompressionModel(CompressionModel *model) {
    vector<char> dict;
    for (int i = 0, count = 256 + (rand() % 64); i < count; i++) {
        dict.push_back('a' + (rand() % 26));
    }
    dict.push_back(0);

    model->setDictionary(&dict[0], dict.size());
    CStringDocumentList docs(&dict[0], NULL);
    model->build(docs);

    vector<pthread_t> threads;
    int ret;

    for (int i = 0; i < 5; i++) {
        threads.resize(threads.size() + 1);
        ret = pthread_create(&threads[threads.size() - 1], 0, runThread, model);
        assertTrue(ret == 0, "Error creating thread");
    }

    for (int i = 0; i < 5; i++) {
        pthread_join(threads[i], 0);
    }
}

void testMultiThreading() {
    PureHuffmanCompressionModel pureHuffman;
    testThreadedCompressionModel(&pureHuffman);
    GZipCompressionModel gzip;
    testThreadedCompressionModel(&gzip);
    GZipDictionaryCompressionModel gzipDict;
    testThreadedCompressionModel(&gzipDict);
    FemtoZipCompressionModel offsetNibble;
    testThreadedCompressionModel(&offsetNibble);
}

void testPrefixHash() {
    {
        string str = "a man a clan a canal panama";
        const char *cstr = str.c_str();
        PrefixHash hash(cstr, str.length(), false);
        for (int i = 0; i < 12; i++) {
            hash.put(cstr + i);
        }

        const char *match;
        int matchLength;

        hash.getBestMatch(cstr + 12, cstr, str.length(), match, matchLength);

        assertTrue(match == cstr + 5, "prefixhash wrong match");
        assertTrue(matchLength == 4, "prefixhash wrong length");
    }

    // With a targetbuf other then the initialized buf
    {
        string str = "a man a clan a canal panama";
        const char *cstr = str.c_str();
        PrefixHash hash(cstr, str.length(), true);
        const char *match;
        int matchLength;

        string target = "xxx a ca";
        const char *targetCstr = target.c_str();
        hash.getBestMatch(targetCstr + 3, targetCstr, target.length(), match, matchLength);

        assertTrue(match == cstr + 12, "prefixhash wrong match");
        assertTrue(matchLength == 5, "prefixhash wrong length");
    }

    // Test a match miss
    {
        string str = "a man a clan a canal panama";
        const char *cstr = str.c_str();
        PrefixHash hash(cstr, str.length(), true);
        const char *match;
        int matchLength;

        string target = "blah!xy";
        const char *targetCstr = target.c_str();
        hash.getBestMatch(targetCstr + 3, targetCstr, target.length(), match, matchLength);

        assertTrue(match == 0, "prefixhash wrong match");
        assertTrue(matchLength == 0, "prefixhash wrong length");
    }

}

const char *get_doc_callback(int doc_index, int *doc_len, void *user_data) {
    char **docs = reinterpret_cast<char **>(user_data);
    *doc_len = strlen(docs[doc_index]);
    return docs[doc_index];
}

void release_doc_callback(const char *buf, void *user_data) {
}

void testCApiCompression(void *model, const char *test_doc) {
    int compressed_capacity = 1024;
    char *compressed = new char[compressed_capacity];
    int compressed_len = fz_compress(model, test_doc, strlen(test_doc), compressed, compressed_capacity);

    assertTrue(compressed_len > 0 && compressed_len < (int)strlen(test_doc), "Compressed doc is not smaller then original");

    int decompressed_capacity = strlen(test_doc);
    char *decompressed = new char[decompressed_capacity];
    int decompressed_len = fz_decompress(model, compressed, compressed_len, decompressed, decompressed_capacity);

    assertTrue(decompressed_len == (int)strlen(test_doc), "Decompressed doc is not the same size as original");
    assertTrue(strncmp(decompressed, test_doc, strlen(test_doc)) == 0, "Decompressed doc is different from original");

    delete[] compressed;
    delete[] decompressed;
}

void testCApi() {
    const char *train_docs[] = {"http://espn.de", "http://popsugar.de", "http://google.de", "http://yahoo.de", "http://www.linkedin.com", "http://www.facebook.com", "http:www.stanford.edu", "http://www.arizona.edu", "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz"};
    int num_docs = 10;

    void *model = fz_build_model(num_docs, get_doc_callback, release_doc_callback, train_docs);

    const char *test_docs[] = {"Check out http://www.facebook.com/me", "http://www.nordstrom.com", "abcdefghijklmnopqrstuvwxyz", 0};
    for (const char **test_doc = test_docs; *test_doc; test_doc++) {
        testCApiCompression(model, *test_doc);
    }

    const char *path = tmpnam(NULL);
    fz_save_model(model, path);
    fz_release_model(model);

    model = fz_load_model(path);
    for (const char **test_doc = test_docs; *test_doc; test_doc++) {
        testCApiCompression(model, *test_doc);
    }

    fz_release_model(model);

    remove(path);
}

extern "C" int exampleCApi(void);

void exampleCApiDriver() {
    assertTrue(exampleCApi() == 0, "exampleCApi failure");
}

void testIntSet() {
    IntSet h;

    assertTrue(h.size() == 0, "empty size");
    h.put(52);
    assertTrue(h.size() == 1, "size 1");
    h.put(52);
    assertTrue(h.size() == 1, "size 1");
    h.put(53);
    assertTrue(h.size() == 2, "size 2");
    h.put(54);
    assertTrue(h.size() == 3, "size 3");
    h.put(54);
    assertTrue(h.size() == 3, "size 3");

    size_t size = 3;

    // force resizing
    for (int i = 100; i < 1000; i++) {
        if (i > 100 && i % 10 == 0) {
            // force a repeat
            h.put(i-1);
        }
        else {
            h.put(i);
            size++;
        }
        assertTrue(h.size() == size, "size");
    }

    h.clear();
    assertTrue(h.size() == 0, "empty size");
}

void testBadModelFiles() {

    // via C api
    void *model = fz_load_model("/tmp/nonexistent123");
    assertTrue(model == 0, "non existent file");

    model = fz_load_model("/dev/null");
    assertTrue(model == 0, "empty file");
}


int main() {

    testDocumentList();

    testDictionaryOptimizerPack();

    testSubstringPacker();

    testPreferNearerMatches();

    testBitIO();

    testHuffman();

    testCompressionModels();

    testDocumentUniquenessScoring();

    testNonexistantStrings();

    testModelOptimization();

    testGZipModel();

    testDataIO();

    testMultiThreading();

    testPrefixHash();

    testCApi();

    exampleCApiDriver();

    testIntSet();

    testBadModelFiles();

    reportTestResults();

	return 0;
}


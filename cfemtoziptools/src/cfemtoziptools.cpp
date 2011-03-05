//============================================================================

#include <iostream>
#include <sstream>
#include <string>
#include <vector>
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
#include <OffsetNibbleHuffmanCompressionModel.h>

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

void reportTestResults() {
    cout << failureCount << " failures" << endl;
    cout << successCount << " successes" << endl;
}

void testDictionaryOptimizerPack() {
    CStringDocumentList docs(PreambleString.c_str(), 0);
    DictionaryOptimizer optimizer(docs);
    string dict = optimizer.optimize(64*1024);
    assertTrue(dict == "ce, in to sticure our, pros of and ity, e the for the establish the United States", string("Dict for US preamble wrong") + dict);

    CStringDocumentList docs2(PanamaString.c_str(), 0);
    DictionaryOptimizer optimizer2(docs2);
    dict = optimizer2.optimize(64*1024);
    assertTrue(dict == "an a ", string("Dict for panama wrong") + dict);
}

void testDocumentList() {
    CStringDocumentList docs("hello", "there", "how are you", 0);

    assertTrue(docs.size() == 3, "Wrong doc count for doclist expected 3, was");
    int length;
    const char *buf = docs.get(0, length);
    assertTrue(length == 5, "Wrong length for doc 0");
    assertTrue(memcmp("hello", buf, length) == 0, "Wrong bytes for doc 0");

    buf = docs.get(1, length);
    assertTrue(length == 5, "Wrong length for doc 1");
    assertTrue(memcmp("there", buf, length) == 0, "Wrong bytes for doc 1");

    buf = docs.get(2, length);
    assertTrue(length == 11, "Wrong length for doc 2");
    assertTrue(memcmp("how are you", buf, length) == 0, "Wrong bytes for doc 2");
}

void testPack(const char *expected, const char *buf, const char *dict = "") {
    {
        SubstringPacker packer(dict ? dict : "", dict ? strlen(dict) : 0);
        VerboseStringConsumer consumer;

        packer.pack(buf, strlen(buf), consumer);
        string out = consumer.getOutput();

        assertTrue(out == expected, "Failure: got '" + out + "' expected '" + expected + "'");

        vector<char> unpackedBytes;
        SubstringUnpacker unpacker(dict ? dict : "", dict ? strlen(dict) : 0, unpackedBytes);
        packer.pack(buf, strlen(buf), unpacker);
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
    testPack("<-3,7>", "aaaaaaa", "aaa");

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

void testBitIO() {
    // Test empty file eof
    {
        istringstream in;
        BitInput bitIn(in);
        int bit = bitIn.readBit();
        assertTrue(bit == -1, "Didn't get eof from empty bit stream");
    }

    // Write 1 bit, read 1 bit
    {
        ostringstream out;
        BitOutput bitOut(out);
        bitOut.writeBit(1);
        bitOut.flush();

        istringstream in(out.str());
        BitInput bitIn(in);
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
        ostringstream out;
        BitOutput bitOut(out);
        for (int i = 0; i < j; i++) {
            bitOut.writeBit(i & 1);
        }
        bitOut.flush();

        istringstream in(out.str());
        BitInput bitIn(in);
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
        ostringstream out;
        BitOutput bitOut(out);
        for (int i = 0; i < 8; i++) {
            bitOut.writeBit(1);
        }
        bitOut.flush();

        istringstream in(out.str());
        BitInput bitIn(in);
        for (int i = 0; i < 8; i++) {
            int bit = bitIn.readBit();
            assertTrue(bit == 1, string("Got wrong bit ") + (bit ? "1" : "0"));
        }
        assertTrue(bitIn.readBit() == -1, "Expected eof from bit input");
    }
}

void huffmanEncodeStringWithModel(vector<int>& data, HuffmanModel& model) {
    ostringstream out;
    HuffmanEncoder encoder(out, model);

    for (vector<int>::iterator i = data.begin(); i != data.end(); i++) {
        encoder.encodeSymbol(*i);
    }
    encoder.finish();

    istringstream in(out.str());
    HuffmanDecoder decoder(in, model);
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

    huffmanEncodeStringWithModel(data, model);
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
                histogram[i] = 20 + (abs(rand()) % 10);
            }

            FrequencyHuffmanModel model(histogram, false);

            vector<int> data(histogram.size());
            for (int i = 0, count = data.size(); i < count; i++) {
                data[i] = abs(rand()) % (histogram.size() - 1); // -1 so we don't emit EOF mid stream!
            }

            huffmanEncodeStringWithModel(data, model);
        }
    }
}

void testModel(const char *source, const char *dictionary, CompressionModel& model, const char *modelName, size_t expectedSize) {
    if (!dictionary) {
        dictionary = "";
    }

    model.setDictionary(dictionary, strlen(dictionary));
    CStringDocumentList documents(source, 0);
    model.build(documents);

    ostringstream out;
    model.compress(source, strlen(source), out);
    string compressed = out.str();

    assertTrue(compressed.length() == expectedSize, string("Wrong compressed size for ") + modelName);

    ostringstream out2;
    model.decompress(compressed.c_str(), compressed.length(), out2);
    string decompressed = out2.str();

    assertTrue(decompressed == source, string("Mismatched string got: '") + decompressed + "' expected '" + source + "' for " + modelName);
}

void testCompressionModels() {
    PureHuffmanCompressionModel pureHuffman;
    testModel(PreambleString.c_str(), PreambleDictionary.c_str(), pureHuffman, "PureHuffman", 211);
    OffsetNibbleHuffmanCompressionModel offsetNibble;
    testModel(PreambleString.c_str(), PreambleDictionary.c_str(), offsetNibble, "OffsetNibble", 205);
}


int main() {

    testDocumentList();

    testDictionaryOptimizerPack();

    testSubstringPacker();

    testBitIO();

    testHuffman();

    testCompressionModels();

    reportTestResults();
	return 0;
}


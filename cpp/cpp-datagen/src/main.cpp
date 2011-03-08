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

/*
 *  Generate sample data simulating a json encoded user payload.  The data is
 *  generated to mimic letter frequencies in english ('e' is more common than
 *  'q') to accurately measure effects of huffman coding.  gzip on individual
 *  payloads has a net *increase* in file size, while the femtozip compressor
 *  compresses to about 33% of size.
 */
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <strings.h>

using namespace std;

static int num;
static string outdir;

struct freq {
    char letter;
    float frequency;
};

static freq freqTable[] = {
    {' ', .06378},
    {'a', .08167},
    {'b', .01492},
    {'c', .02782},
    {'d', .04253},
    {'e', .12702},
    {'f', .02228},
    {'g', .02015},
    {'h', .06094},
    {'i', .06966},
    {'j', .00153},
    {'k', .00772},
    {'l', .04025},
    {'m', .02406},
    {'n', .06749},
    {'o', .07507},
    {'p', .01929},
    {'q', .00095},
    {'r', .05987},
    {'s', .06327},
    {'t', .09056},
    {'u', .02758},
    {'v', .00978},
    {'w', .02360},
    {'x', .00150},
    {'y', .01974},
    {'z', .0074},
};

char generateRandomChar() {
    float r = (rand() % 10000) / 10000.0;
    float sum = 0;
    for (int i = 0; i < 27; i++) {
        sum += freqTable[i].frequency;
        if (r < sum) {
            return freqTable[i].letter;
        }
    }
    return 'e';
}

string generateRandomString(int minLen, int maxLen) {
    string str;
    for (int i = 0, len = (rand() % (maxLen - minLen + 1)) + minLen; i < len; i++) {
        str.push_back(generateRandomChar());
    }
    return str;
}

void parseArgs(int argc, char **argv) {
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--num")) {
            stringstream ss(argv[++i]);
            ss >> num;
        }
        else {
            outdir = argv[i];
        }
    }

    if (num < 1) {
        fprintf(stderr, "usage: --num number [outputdir]\n");
        exit(1);
    }
}

string generateRandomEmail() {
    string domain;

    // Capture effects of popular domains
    int i = (rand() % 7);
    if (i == 0) {
        domain = "yahoo.com";
    }
    else if (i == 1) {
        domain = "gmail.com";
    }
    else if (i == 2) {
        domain = "hotmail.com";
    }
    else {
        domain = generateRandomString(6,14);
        if (i == 3) {
            domain += ".org";
        }
        else if (i == 4) {
            domain += ".edu";
        }
        else {
            domain += ".com";
        }
    }

    return generateRandomString(6,14) + "@" + domain;
}

string generateRandomBirthday() {
    int year = 2011 - ((rand() % 50) + 10);
    int month = (rand() % 12) + 1;
    int day = (rand() % 30) + 1;

    ostringstream o;
    o << year << "-" << month << "-" << day;
    return o.str();
}

void generate() {
    for (int i = 1; i <= num; i++) {
        string first = generateRandomString(6,14);
        string last = generateRandomString(6,14);
        string email = generateRandomEmail();
        string bday = generateRandomBirthday();
        string gender = (rand() % 2) == 0 ? "m" : "f";

        ostream * out;

        if (outdir != "") {
            ostringstream pathStream;
            pathStream << outdir << "/" << i;
            string path = pathStream.str();
            out = new ofstream(path.c_str(), ios_base::trunc);
        }
        else {
            out = &cout;
        }

        (*out) << "{\"first\":\"" << first << "\",\"last\":\"" << last << "\",\"email\":\"" << email << "\",\"gender\":\"" << gender << "\",\"bday\":\"" << bday << "\"}" << endl;

        if (outdir != "") {
            static_cast<ofstream*>(out)->close();
        }
    }
}

int main(int argc, char** argv) {
    parseArgs(argc, argv);

    generate();

    return 0;
}

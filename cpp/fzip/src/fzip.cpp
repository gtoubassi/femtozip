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

#include <dirent.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <string>
#include <sys/stat.h>
#include <sys/time.h>
#include <vector>

#include <DataIO.h>
#include <FileDocumentList.h>
#include <FileUtil.h>
#include <OptimizingCompressionModel.h>


using namespace std;
using namespace femtozip;

enum Operation {
    None, Build, Compress, Decompress
};

string modelPath;
Operation operation = None;
vector<string> models;
vector<string> paths;
bool verbose = false;
bool preload = false;

long getTimeMillis() {
    timeval tim;
    gettimeofday(&tim, NULL);
    return tim.tv_sec * 1000 + tim.tv_usec / 1000;
}

CompressionModel *loadModel() {
    ifstream file(modelPath.c_str(), ios::in | ios::binary);
    DataInput in(file);
    CompressionModel *model = CompressionModel::loadModel(in);
    file.close();
    return model;
}

void saveModel(CompressionModel& model) {
    ofstream file(modelPath.c_str(), ios::out | ios::binary | ios::trunc);
    DataOutput out(file);
    CompressionModel::saveModel(model, out);
    out.flush();
    file.close();
}


void buildModel() {
    vector<string> pass1Paths(paths.begin(), paths.begin() + paths.size() / 2);
    FileDocumentList pass1Docs(pass1Paths);

    vector<string> pass2Paths(paths.begin() + paths.size() / 2 + 1, paths.end());
    FileDocumentList pass2Docs(pass2Paths);

    // Pass1 builds the models
    OptimizingCompressionModel model(models);

    long start = getTimeMillis();

    cout << "Building model..." << endl;
    model.build(pass1Docs);

    // Pass2 picks the best one
    model.optimize(pass2Docs);

    long duration = getTimeMillis() - start;
    if (verbose) {
        cout <<"Done in " << setprecision(6) << (duration / 1000.0) << "s" << endl;
    }

    saveModel(*model.getBestPerformingModel());
}

void compress() {
    CompressionModel *model = loadModel();

    long duration = 0;
    for (vector<string>::iterator i = paths.begin(); i != paths.end(); i++) {

        if (verbose) {
            cout << "Compressing " << *i << endl;
        }

        int length;
        const char *buf = FileUtil::readFully(i->c_str(), length);

        ostringstream outstr;

        long start = getTimeMillis();
        model->compress(buf, length, outstr);
        duration += (getTimeMillis() - start);
        string compressedData = outstr.str();

        string compressedFile = *i + ".fz";
        ofstream file(compressedFile.c_str(), ios::out | ios::binary | ios::trunc);
        if (file.is_open()) {
            file.write(compressedData.c_str(), compressedData.length());
            if (file.good()) {
                remove(i->c_str());
            }
            file.close();
        }

        delete[] buf;
    }

    if (verbose) {
        cout <<"Done in " << setprecision(6) << (duration / 1000.0) << "s" << endl;
    }

    delete model;
}

void decompress() {
    CompressionModel *model = loadModel();

    long duration = 0;
    for (vector<string>::iterator i = paths.begin(); i != paths.end(); i++) {

        if (verbose) {
            cout << "Decompressing " << *i << endl;
        }

        int length;
        const char *buf = FileUtil::readFully(i->c_str(), length);

        ostringstream outstr;

        long start = getTimeMillis();
        model->decompress(buf, length, outstr);
        duration += (getTimeMillis() - start);
        string decompressedData = outstr.str();

        string compressedFile = *i;
        string uncompressedFile = compressedFile;
        if (compressedFile.length() > 3) {
            size_t extension = compressedFile.find_last_of(".fz", string::npos, 3);
            if (extension == compressedFile.length() - 1) {
                uncompressedFile = compressedFile.substr(0, extension - 2);
            }
        }
        ofstream file(uncompressedFile.c_str(), ios::out | ios::binary | ios::trunc);
        file.write(decompressedData.c_str(), decompressedData.length());
        if (file.good() && uncompressedFile != compressedFile) {
            remove(i->c_str());
        }
        file.close();

        delete[] buf;
    }

    if (verbose) {
        cout <<"Done in " << setprecision(6) << (duration / 1000.0) << "s" << endl;
    }

    delete model;
}


void usage(const string& error = "") {
    if (error.length() > 0) {
        cout << error << endl << endl;
    }
    cout << "usage: --model <path> --build|compress|decompress [--models model1,model2,...] [--verbose] [--preload] <path> ..." << endl;
    cout << "       if path is a directory all files in the specified directory are used" << endl;
    if (error.length() > 0) {
        exit(1);
    }
}

void parseArgs(int argc, const char **argv) {
    for (int i = 1; i < argc; i++) {
        const char *arg = argv[i];

        if (strcmp("--model", arg) == 0) {
            if (i < argc - 1 && strncmp("--", argv[i + 1], 2) != 0) {
                modelPath = argv[++i];
            }
            else {
                usage("--model must be followed by a path");
            }
        }
        else if (strcmp("--build", arg) == 0) {
            operation = Build;
        }
        else if (strcmp("--compress", arg) == 0) {
            operation = Compress;
        }
        else if (strcmp("--decompress", arg) == 0) {
            operation = Decompress;
        }
        else if (strcmp("--verbose", arg) == 0) {
            verbose = true;
        }
        else if (strcmp("--preload", arg) == 0) {
            preload = true;
        }
        else if (strcmp("--models", arg) == 0) {
            string modelNames = argv[++i];
            size_t pos = 0;
            while (pos != string::npos) {
                size_t pos2 = modelNames.find(",", pos);
                models.push_back(modelNames.substr(pos, pos2 == string::npos ? pos2 : pos2 - 1));
                pos = pos2;
            }
        }
        else if (strncmp("--", arg, 2) == 0) {
            usage(string("Unknown argument ") + arg);
        }
        else {
            struct stat statBuf;

            if (stat(arg, &statBuf) != 0) {
                usage(string("Cannot stat ") + arg);
            }

            if (S_ISDIR(statBuf.st_mode)) {
                DIR *dirp;
                struct dirent *dp;

                if ((dirp = opendir(arg)) == NULL) {
                    usage(string("Cannot opendir ") + arg);
                }

                while ((dp = readdir(dirp)) != NULL) {
                    if (dp->d_type == DT_REG) {
                        paths.push_back(string(arg) + "/" + dp->d_name);
                    }
                }

                closedir(dirp);
            }
            else {
                paths.push_back(string(arg));
            }
        }
    }

    if (operation == None) {
        usage("Must specify one of --compress, --decompress, or --build");
    }
    if (modelPath.length() == 0) {
        usage("Must specify --model <path>");
    }

    if (paths.size() == 0) {
        usage("Must specify at least one path to operate on");
    }
}


int main(int argc, const char **argv) {
    parseArgs(argc, argv);

    switch (operation) {
    case Build:
        buildModel();
        break;
    case Compress:
        compress();
        break;
    case Decompress:
        decompress();
        break;
    case None:
        throw "Should never happen";
        break;
    }

    return 0;
}

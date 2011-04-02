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

#include "SamplingDocumentList.h"
#include <algorithm>

using namespace std;

namespace femtozip {

SamplingDocumentList::SamplingDocumentList(DocumentList& documents, int numPartitions, int partition) : documents(documents), numPartitions(numPartitions), partition(partition) {
}

SamplingDocumentList::~SamplingDocumentList() {
}

int SamplingDocumentList::size() {
    int docsSize = documents.size();
    return max(1, docsSize / numPartitions + ((docsSize / numPartitions) > partition ? 1 : 0));
}

const char *SamplingDocumentList::get(int i, int& length) {
    i = min(documents.size() - 1, i * numPartitions + partition);
    return documents.get(i, length);
}

void SamplingDocumentList::release(const char *buf) {
    documents.release(buf);
}


}

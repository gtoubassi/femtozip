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

#ifndef SAMPLINGDOCUMENTLIST_H_
#define SAMPLINGDOCUMENTLIST_H_

#include "DocumentList.h"

namespace femtozip {

class SamplingDocumentList : public DocumentList {
protected:
    DocumentList& documents;
    int numPartitions;
    int partition;

public:
    SamplingDocumentList(DocumentList& documents, int numPartitions, int partition);
    virtual ~SamplingDocumentList();

    virtual int size();
    virtual const char *get(int i, int& length);
    virtual void release(const char *buf);
};

}

#endif /* SAMPLINGDOCUMENTLIST_H_ */

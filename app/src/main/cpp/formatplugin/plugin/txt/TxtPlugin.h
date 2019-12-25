//
// Created by 陈广祥 on 2019-09-19.
//

#ifndef NBREADER_TXTPLUGIN_H
#define NBREADER_TXTPLUGIN_H


#include <reader/book/Book.h>
#include <reader/bookmodel/BookModel.h>
#include "../FormatPlugin.h"

class TxtPlugin : public FormatPlugin {
public:
    TxtPlugin();

    ~TxtPlugin();

    bool readEncodingInternal(std::string &outEncoding) override;

    bool readLanguageInternal(std::string &outLanguage) override;

    bool readChaptersInternal(std::string &chapterPattern,
                              std::vector<TextChapter> &chapterList) override;

    bool
    readChapterContentInternal(TextChapter &txtChapter, char **outBuffer, size_t outSize) override;

};

#endif //NBREADER_TXTPLUGIN_H

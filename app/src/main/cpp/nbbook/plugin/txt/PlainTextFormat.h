// author : newbiechen
// date : 2019-09-24 14:12
// description : 文本工具
//

#ifndef NBREADER_PLAINTXTFORMAT_H
#define NBREADER_PLAINTXTFORMAT_H


#include "../../filesystem/File.h"
#include "../../filesystem/io/InputStream.h"

// 纯文本参数信息
class PlainTextFormat {
public:
    // 允许更换段落的条件
    enum ParagraphBreakType {
        BREAK_PARAGRAPH_AT_NEW_LINE = 1,
        BREAK_PARAGRAPH_AT_EMPTY_LINE = 2,
        BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4,
    };

    PlainTextFormat();

    ~PlainTextFormat() {}

    // 是否初始化
    bool hasInitialized() const { return isInitialized; }

    // 获取换行类型 (允许多选,如：BREAK_PARAGRAPH_AT_NEW_LINE | BREAK_PARAGRAPH_AT_EMPTY_LINE)
    int getBreakType() const { return mBreakType; }

    // 获取无视缩进的距离
    int getIgnoredIndent() const { return mIgnoredIndent; }

private:
    bool isInitialized;
    int mBreakType;
    int mIgnoredIndent;

    // 友元:文本探测器
    friend class PlainTextDetector;
};

// 纯文本探测器，用于探测文本初始化 PlainTextFormat 配置信息
class PlainTextDetector {
public:
    PlainTextDetector() {}

    ~PlainTextDetector() {}

    void detect(File &file, PlainTextFormat &format);

    void detect(InputStream &inputStream, PlainTextFormat &format);
};


#endif //NBREADER_PLAINTXTFORMAT_H

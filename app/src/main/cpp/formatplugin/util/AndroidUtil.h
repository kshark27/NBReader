//
// Created by 陈广祥 on 2019-09-19.
// desc：C++ 调用 Java 层的工具

#ifndef NBREADER_ANDROIDUTIL_H
#define NBREADER_ANDROIDUTIL_H


#include <jni.h>
#include <string>
#include <vector>
#include "JNIEnvelope.h"

class AndroidUtil {
private:
    static JavaVM *sJavaVM;

public:
    static JavaClass Class_String;
    static JavaClass Class_Locale;

    static JavaClass Class_NativeFormatPlugin;
    static JavaClass Class_BoolPluginManager;
    static JavaClass Class_BookModel;
    static JavaClass Class_BookEntity;
    static JavaClass Class_TextModel;
    // 编码转换器
    static JavaClass Class_EncodingConverter;
    // 章节探测器
    static JavaClass Class_ChapterDetector;

    static std::shared_ptr<StaticObjectMethod> StaticMethod_Locale_getDefault;
    static std::shared_ptr<StaticBooleanMethod> StaticMethod_EncodingConverter_isEncodingSupport;
    static std::shared_ptr<StaticObjectMethod> StaticMethod_EncodingConverter_createEncodingConverter;

    static std::shared_ptr<StaticObjectMethod> StaticMethod_ChapterDetector_createChapterDetector;


    static std::shared_ptr<StringMethod> Method_Locale_getLanguage;

    static std::shared_ptr<StringMethod> Method_String_toLowerCase;
    static std::shared_ptr<StringMethod> Method_String_toUpperCase;

    static std::shared_ptr<ObjectMethod> Method_BookModel_getBook;
    static std::shared_ptr<StringMethod> Method_Book_getTitle;
    static std::shared_ptr<StringMethod> Method_Book_getUrl;
    static std::shared_ptr<StringMethod> Method_Book_getEncoding;
    static std::shared_ptr<StringMethod> Method_Book_getLang;
    static std::shared_ptr<ObjectMethod> Method_BookModel_createTextModel;
    static std::shared_ptr<VoidMethod> Method_BookModel_setTextModel;

    // JavaEncodingConverter
    static std::shared_ptr<StringMethod> Method_EncodingConverter_getName;
    static std::shared_ptr<IntMethod> Method_EncodingConverter_convert;
    static std::shared_ptr<VoidMethod> Method_EncodingConverter_reset;

    static std::shared_ptr<StringMethod> Method_ChapterDetector_getRegexStr;


public:
    static bool init(JavaVM *jvm);

    static JNIEnv *getEnv();

    static std::string toCString(JNIEnv *env, jstring from);

    static jstring toJString(JNIEnv *env, const std::string &str);

    static std::string convertNonUtfString(const std::string &str);

/*    static jintArray createJavaIntArray(JNIEnv *env, const std::vector<jint> &data);

    static jbyteArray createJavaByteArray(JNIEnv *env, const std::vector<jbyte> &data);*/
};

#endif //NBREADER_ANDROIDUTIL_H

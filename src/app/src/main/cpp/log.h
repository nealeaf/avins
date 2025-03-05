//
// Created by macro on 2025/3/4.
//

#ifndef AVINS_LOG_H
#define AVINS_LOG_H
#include <android/log.h>

#define DEBUG // 可以通过 CmakeLists.txt 等方式来定义在这个宏，实现动态打开和关闭LOG

// Windows 和 Linux 这两个宏是在 CMakeLists.txt 通过 ADD_DEFINITIONS 定义的
#ifdef Windows
#define __FILENAME__ (strrchr(__FILE__, '\\') + 1) // Windows下文件目录层级是'\\'
#elif Linux
#define __FILENAME__ (strrchr(__FILE__, '/') + 1) // Linux下文件目录层级是'/'
#else
#define __FILENAME__ (strrchr(__FILE__, '/') + 1) // 默认使用这种方式
#endif

#ifdef DEBUG
#define TAG "AVINS"
#define LOGV(format, ...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,\
        "[%s][%d]: " format, __FILENAME__,  __LINE__, ##__VA_ARGS__);
#define LOGD(format, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG,\
        "[%s][%d]: " format, __FILENAME__,  __LINE__, ##__VA_ARGS__);
#define LOGI(format, ...) __android_log_print(ANDROID_LOG_INFO, TAG,\
        "[%s][%d]: " format, __FILENAME__,  __LINE__, ##__VA_ARGS__);
#define LOGW(format, ...) __android_log_print(ANDROID_LOG_WARN, TAG,\
        "[%s][%d]: " format, __FILENAME__,  __LINE__, ##__VA_ARGS__);
#define LOGE(format, ...) __android_log_print(ANDROID_LOG_ERROR, TAG,\
        "[%s][%d]: " format, __FILENAME__,  __LINE__, ##__VA_ARGS__);
#else
#define LOGV(format, ...);
#define LOGD(format, ...);
#define LOGI(format, ...);
#define LOGW(format, ...);
#define LOGE(format, ...);
#endif // DEBUG

#endif //AVINS_LOG_H

#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include "vins.h"
#include "log.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_macro_avins_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_macro_avins_MainActivity_pushImage(
        JNIEnv* env,
        jobject /* this */,
        jlong timestamp,
        jobject bitmap) {
    if (nullptr == bitmap) {
        return -1;
    }

    AndroidBitmapInfo info; // create a AndroidBitmapInfo
    int ret;
    // 获取图片信息
    ret = AndroidBitmap_getInfo(env, bitmap, &info);
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo failed, result: %d", ret);
        return ret;
    }

    LOGD("bitmap width: %d, height: %d, format: %d, stride: %d", info.width, info.height,
         info.format, info.stride);

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return -2;
    }

    // 获取像素信息
    unsigned char *addrPtr;
    ret = AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&addrPtr));
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels failed, result: %d", ret);
        return ret;
    }
    cv::Mat rgba(info.height, info.width, CV_8UC4, addrPtr);
    cv::Mat image;
    cv::cvtColor(rgba, image, cv::COLOR_BGRA2BGR);

    // 像素信息不再使用后需要解除锁定
    ret = AndroidBitmap_unlockPixels(env, bitmap);
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_unlockPixels failed, result: %d", ret);
        return ret;
    }
    Avins::Vins::Instance().PushImage(timestamp, image);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_macro_avins_MainActivity_pushImu(
        JNIEnv* env,
        jobject /* this */,
        jlong timestamp,
        jfloat accX,
        jfloat accY,
        jfloat accZ,
        jfloat gyrX,
        jfloat gyrY,
        jfloat gyrZ) {
    Avins::Vins::Instance().PushImu(timestamp, accX, accY, accZ, gyrX, gyrY, gyrZ);
    return 0;
}
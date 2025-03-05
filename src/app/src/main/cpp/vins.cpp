//
// Created by admin on 2025/3/3.
//
#include "vins.h"
#include "log.h"

#define FPS 5

namespace Avins {
    int Vins::PushImage(long timestamp, cv::Mat &image) {
        if (image.empty()) {
            LOGE("push image is null");
            return -1;
        }
        static long timestamp_pre = timestamp;
        static int count = 0;
        count++;
        if (timestamp_pre <= timestamp || (timestamp - timestamp_pre) < (count * 1e9 / FPS))
        {
            return 1;
        }
        timestamp_pre = timestamp;
        MsgImagePtr imagePtr(new MsgImage());
        imagePtr->header = 1.f/1e9 * timestamp;
        imagePtr->image = image;

//        std::lock_guard<std::mutex> lg(lockImage_);
//        deqImage_.emplace_back(imagePtr);
//        cvImage_.notify_one();

        return 0;
    }

    int Vins::PushImu(long timestamp, float accX, float accY, float accZ, float gyrX, float gyrY,
                      float gyrZ) {
        MsgImuPtr imuPtr(new MsgImu());
        imuPtr->header = 1.f/1e9 * timestamp;
        Eigen::Vector3d vAcc = {accX, accY, accZ};
        imuPtr->linear_acceleration = vAcc;
        Eigen::Vector3d vGyr = {gyrX, gyrY, gyrZ};
        imuPtr->angular_velocity = vGyr;

        if (timestamp_last_ <= timestamp)
        {
            LOGI("imu message in disorder!");
            return -1;
        }
        timestamp_last_ = timestamp;

//        std::lock_guard<std::mutex> lg(lockImu_);
//        deqImu_.emplace_back(imuPtr);
//        cvImu_.notify_one();
        return 0;
    }
} // Avins
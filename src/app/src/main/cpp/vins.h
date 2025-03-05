//
// Created by macro on 2025/3/3.
//

#ifndef AVINS_VINS_H
#define AVINS_VINS_H

#include <mutex>
#include <thread>
#include <condition_variable>
#include "eigen3/Eigen/Eigen"
#include "opencv2/opencv.hpp"
//#include "vins/include/estimator.h"
//#include "vins/include/parameters.h"
//#include "vins/include/feature_tracker.h"

namespace Avins {

    struct MsgImu {
        double header;
        Eigen::Vector3d linear_acceleration;
        Eigen::Vector3d angular_velocity;
    };
    typedef std::shared_ptr<MsgImu> MsgImuPtr;

    struct MsgImage {
        double header;
        cv::Mat image;
    };
    typedef std::shared_ptr<MsgImage> MsgImagePtr;

    struct MsgFeature {
        double header;
        std::vector<Eigen::Vector3d> points;
        std::vector<int> id_of_point;
        std::vector<float> u_of_point;
        std::vector<float> v_of_point;
        std::vector<float> velocity_x_of_point;
        std::vector<float> velocity_y_of_point;
    };
    typedef std::shared_ptr<MsgFeature> MsgFeaturePtr;

    class Vins {
    public:
        static Vins &Instance() {
            static Vins inst;
            return inst;
        }

        int PushImage(long timestamp, cv::Mat &image);

        int PushImu(long timestamp, float accX, float accY, float accZ, float gyrX, float gyrY,
                    float gyrZ);

        void ProcessBackEnd();
        void Draw();

        ~Vins() {};
    private:
        Vins() {};

//        FeatureTracker tracker_;
//        Estimator estimator_;

        long timestamp_last_;
        std::mutex lockImu_;
        std::condition_variable cvImu_;
        std::deque<MsgImuPtr> deqImu_;
        std::mutex lockImage_;
        std::condition_variable cvImage_;
        std::deque<MsgImagePtr> deqImage_;
    };
} // Avins
#endif // AVINS_VINS_H

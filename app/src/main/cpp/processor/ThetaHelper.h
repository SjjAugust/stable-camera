//
// Created by 张哲华 on 2019/3/1.
//

#ifndef GRYOSTABLE_THETAHELPER_H
#define GRYOSTABLE_THETAHELPER_H

#include "opencv2/opencv.hpp"
#include "ThreadContext.h"
#include "Filter.h"
using namespace cv;
using namespace std;
class ThetaHelper {
private:
//    Mat inmat=(cv::Mat_<double>(3, 3)<<1440.0,0.0,540.0, 0.0,1440.0,960.0,0.0,0.0,1.0);
//    Mat inmat=(cv::Mat_<double>(3, 3)<<600.0,0.0,616.0, 0.0,600.0,956.0,0.0,0.0,1.0);
    Mat inmat=(cv::Mat_<double>(3, 3)<<1430.2,0.0,505.7, 0.0,1422.9,922.1,0.0,0.0,1.0);//OnePlus 6T
    Mat vertex=(cv::Mat_<double>(3, 4)<<0.0,0.0,1080.0,1080.0,0.0,1920.0,1920.0,0.0,1.0,1.0,1.0,1.0);
    Mat cropvertex=(cv::Mat_<double>(3, 4)<<108.0,108.0,972.0, 972.0, 192.0 , 1728.0, 1728.0, 192.0,1.0,1.0,1.0,1.0);
    Mat I=(cv::Mat_<double>(3, 3)<<1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0);


    int c;
    int gyindex;
    int findex;
    int angledex;

    vector<double> roxl;
    vector<double> royl;
    vector<double> rozl;
    vector<double> Timeg;
    vector<double> Timeframe;
    vector<double> oldx;
    vector<double> oldy;
    vector<double> oldz;
    double lastx;
    double lasty;
    double lastz;


    cv::Mat result ;
    cv::Mat RR;

    double px;
    double py;
    double pz;
    double q;
    double rx;
    double ry;
    double rz;
    double kx;
    double ky;
    double kz;
    cv::Vec<double, 3> lasttheta;
    cv::Vec<double, 3> lastt;
    void cropControl(Mat& RR);
    bool isInside(cv::Mat cropvertex, cv::Mat newvertex);

public:
    void init();
    Mat getRR(Mat oldRotation, Mat newRotation);
    cv::Vec<double, 3> getTheta();
    cv::Vec<double, 3> getNewTheta(cv::Vec<double, 3> oldtheta);
    cv::Mat getRotationMat(cv::Vec<double, 3> theta);
    void getR(double timestamp, Mat *matR, bool isCrop);
    void putValue(double timestamp, float x, float y, float z);

private:
    int rs_frame_index_;
    int rs_gyro_index_;
    cv::Vec<double, 4> rs_last_theta_;
    double rs_last_x_, rs_last_y_, rs_last_z_;
    std::vector<cv::Vec<double, 4>> rs_gyro_theta_;
    const bool is_use_drift_ = false;
    const float x_drift_ = -0.945954f;
    const float y_drift_ = 0.227967;
    const float z_drift_ = 0.018109f;
    Filter filter_;
    std::queue<cv::Mat> old_rotation_queue_;
public:
    std::vector<cv::Vec<double, 4>> GetRsTheta();
    void RsChangeVectorToMat(cv::Mat* rs_out_Mat);
};


#endif //GRYOSTABLE_THETAHELPER_H

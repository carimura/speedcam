// cv_wrapper.cpp
#include <opencv2/opencv.hpp>
#include <opencv2/objdetect.hpp>

extern "C" {

// Detect faces and draw rectangles. Returns 1 on success, 0 on failure.
int face_detect(const char* imagePath, const char* cascadePath, const char* outPath) {
    cv::Mat imgIn = cv::imread(imagePath);
    if (imgIn.empty()) return 0;

    cv::CascadeClassifier faceDetector;
    if (!faceDetector.load(cascadePath)) return 0;

    std::vector<cv::Rect> faces;
    faceDetector.detectMultiScale(imgIn, faces);

    for (const auto& rect : faces) {
        cv::rectangle(
            imgIn,
            cv::Point(rect.x, rect.y),
            cv::Point(rect.x + rect.width, rect.y + rect.height),
            cv::Scalar(0, 0, 255),
            5
        );
    }

    return cv::imwrite(outPath, imgIn) ? 1 : 0;
}

// Load image and return its width
int get_image_width(const char* path) {
    cv::Mat img = cv::imread(path);
    if (img.empty()) {
        return -1; // Error loading image
    }
    return img.cols;
}


}
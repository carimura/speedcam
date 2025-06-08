# SpeedCam

Exploration of automated speed detection in modern Java, starting with the OpenCV JNI bindings and then switching to the FFM API.

## TODO
- add the time of the car itself into MotionResult which can be determined from the filename (Also need to change test files to add this file format)
- process all files in an entire directory
- Debug right to left (some off by ~15 frames issue)
- Test suite that checks known cars (this video SHOULD BE 23mph, LeftToRight, 319 frames, etc)
- Front end CRUD site
- Convert to FFM API (calc performance differences)


## Building
mvn clean -Pcomplete package

## Running
java --enable-preview -cp ./target/speedcam-0.1-jar-with-dependencies.jar com.pinealpha.SpeedDetect --in /sample_videos/left_to_right_1.mp4 --debug

## Filename format
Road Cam 6-4-2025, 1.10.33pm PDT - 6-4-2025, 1.10.33pm PDT.mp4



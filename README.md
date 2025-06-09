# SpeedCam

Exploration of automated speed detection in modern Java, starting with the OpenCV JNI bindings and then switching to the FFM API.

## TODO
- Still not detecting properly for both left and right.....
- The input video path -- is that in the built jar or can we process external file?
- see if there's any simple refactorings of SpeedDetect.java, getting a little long in the tooth
- add the time of the car itself into MotionResult which can be determined from the filename (Also need to change test files to add this file format)
- process all files in an entire directory
- Test suite that checks known cars (this video SHOULD BE 23mph, LeftToRight, 319 frames, etc)
- Front end CRUD site
- Convert to FFM API (calc performance differences)


## Building
mvn clean -Pcomplete package

## Running
java --enable-preview -cp ./target/speedcam-0.1-jar-with-dependencies.jar com.pinealpha.SpeedDetect --in /sample_videos/left_to_right_1.mp4 --debug

## Filename format
Road Cam 6-4-2025, 1.10.33pm PDT - 6-4-2025, 1.10.33pm PDT.mp4


## Debugging
- left_to_right_1 --> motion frames 91-199
- right_to_left_1 --> motion frames 158-320
- right_to_left_2 --> motion frames 153-345
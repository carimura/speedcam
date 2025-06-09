# SpeedCam

Exploration of automated speed detection in modern Java, starting with the OpenCV JNI bindings and then switching to the FFM API.

## TODO
- Test suite that checks known cars (this video SHOULD BE 23mph, LeftToRight, 319 frames, etc)
- Still not detecting properly for both left and right..... maybe just ignore right to left completely... seems reasonable
- The input video path -- is that in the built jar or can we process external file?
- see if there's any simple refactorings of SpeedDetect.java, getting a little long in the tooth
- add the time of the car itself into MotionResult which can be determined from the filename (Also need to change test files to add this file format)
- process all files in an entire directory
- Front end CRUD site
- Convert to FFM API (calc performance differences)


## Building
mvn clean -Pcomplete package

## Running
java --enable-preview -cp ./target/speedcam-0.1-jar-with-dependencies.jar com.pinealpha.SpeedDetect --in /sample_videos/left_to_right_1.mp4 --debug

## Filename format
Road Cam 6-4-2025, 1.10.33pm PDT - 6-4-2025, 1.10.33pm PDT.mp4


## Testing and Debugging
- left-to-right-1 --> motion frames should be 61-178
- left-to-right-2 --> motion frames should be 72-233
- right-to-left-1 --> motion frames should be 89-279
- right-to-left-2 --> motion frames should be 133-249
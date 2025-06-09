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


## Target Motion Results
- left-to-right-1 --> motion frames should be 61-178
- left-to-right-2 --> motion frames should be 72-233
- right-to-left-1 --> motion frames should be 89-279
- right-to-left-2 --> motion frames should be 133-249


## Getting More Accurate

0. We are going to run SpeedDetect across some known sample videos and compare the results we get to the "Target Motion Results" listed above
1. process the files in /src/main/resources/sample_videos/
2. check the motion results for each one, the first motion frame and last motion frame should be within 5 frames of the target results above
3. If they are not, we need to tweak the various settings and sensitivies until our results match the target results for all 4 videos.
4. Can you do this in a loop? I will take myself out of the loop. I have committed all current code to GitHub so you can make changes until you are complete without messing things up.
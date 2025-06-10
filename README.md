# SpeedCam

Exploration of automated speed detection in modern Java, starting with the OpenCV JNI bindings and then switching to the FFM API.

## TODO
- add the time of the car itself into MotionResult which can be determined from the filename
- see if there's any simple refactorings of SpeedDetect.java, getting a little long in the tooth
- Front end CRUD site
- Convert to FFM API (calc performance differences)


## Building
`mvn clean -Pcomplete package`

## Running
`java --enable-preview -cp ./target/speedcam-0.1-jar-with-dependencies.jar com.pinealpha.SpeedDetect --in src/main/resources/sample_videos/`

## Filename format
Road Cam 6-4-2025, 1.10.33pm PDT - 6-4-2025, 1.10.33pm PDT.mp4


## Target Motion Results
- left-to-right-1 --> motion frame range should be 61-178
- left-to-right-2 --> motion frame range should be 72-233
- left-to-right-3 --> motion frame range should be 74-196
- left-to-right-4 --> motion frame range should be 44-162
- right-to-left-1 --> motion frame range should be 89-279
- right-to-left-2 --> motion frame range should be 133-249
- right-to-left-3 --> motion frame range should be 165-351
- right-to-left-4 --> motion frame range should be 66-220


## Testing

All videos
`mvn test`

Single video
`mvn test -Dvideo.test.filter=right-to-left-2`



## Testing instructions for LLM

0. We are going to build and run SpeedDetect across some known sample videos and compare the results we get to the "Target Motion Results" listed above
1. The files we are going to process are all in /src/main/resources/sample_videos/
2. After the full run, let's check the motion results for each one, and compare to the target motion results above. The first motion frame and last motion frame should be within 10 frames of the target results above. If they are, then we can consider that a passing test. If they are not, that is a failed test.
3. A rejected video is OK, we won't call that a fail just a "SKIP"
# SpeedCam

Exploration of automated speed detection in modern Java, starting with the OpenCV JNI bindings and then switching to the FFM API.

## TODO
- currently performance is an issue... it'll take over 3 hours to process a days worth of videos. Need to speed this up by a lot. Parallelize? GPUs? Panama conversion to FFM API?
- Currently thinking offload to a Function compute service to parallelize across cloud compute. 
- Front end CRUD site
- Convert to FFM API (calc performance differences)


## Building
`mvn clean -Pcomplete package`
`mvn clean -Pcomplete package -Dmaven.test.skip`

## Running
`java --enable-preview -cp ./target/speedcam-0.1-jar-with-dependencies.jar com.pinealpha.SpeedDetect --in src/main/resources/sample_videos/`
`java --enable-preview -cp ./target/speedcam-0.1-jar-with-dependencies.jar com.pinealpha.SpeedDetect --in 'src/main/resources/sample_videos/Road Cam 6-7-2025, 2.05.46pm PDT - 6-7-2025, 2.05.46pm PDT.mp4'`

## Filename format
Road Cam 6-4-2025, 1.10.33pm PDT - 6-4-2025, 1.10.33pm PDT.mp4


## Target Motion Results
- Road Cam 6-7-2025, 2.05.46pm --> motion frame range should be 61-178 left-to-right
- Road Cam 6-7-2025, 2.09.59pm --> motion frame range should be 72-233 left-to-right
- Road Cam 6-9-2025, 1.01.52pm --> motion frame range should be 74-196 left-to-right
- Road Cam 6-9-2025, 9.57.54am --> motion frame range should be 44-162 left-to-right
- Road Cam 6-7-2025, 2.39.30pm --> motion frame range should be 89-279 right-to-left
- Road Cam 6-9-2025, 7.55.13am --> motion frame range should be 133-249 right-to-left
- Road Cam 6-9-2025, 1.08.44pm --> motion frame range should be 165-351 right-to-left
- Road Cam 6-9-2025, 10.43.09am --> motion frame range should be 66-220 right-to-left


## Testing

All videos
`mvn clean test`

Single video
`mvn clean test -Dvideo.test.filter='Road Cam 6-7-2025, 2.05.46pm'`

## Performance

### Test Runtimes
- Total time to run all tests: 1m 26s
- Average time per video (just the video): 8.7 seconds

### OpenCV Operation Timings

The following table shows the total time spent in each of the major OpenCV operations across all 8 test videos.

| Operation              | Total Time (ms) |
| ---------------------- | --------------- |
| Background Subtraction | 30,419.03       |
| Frame Reading          | 21,839.78       |
| Contour Detection      | 11,718.50       |
| Image Filtering        | 4,035.28        |
| Contour Processing     | 214.32          |

Road Cam 6-9-2025, 1.01.52pm: 10,630 ms
Road Cam 6-7-2025, 2.05.46pm: 9,245 ms
Road Cam 6-7-2025, 2.09.59pm: 11,029 ms
Road Cam 6-9-2025, 10.43.09am: 9,498 ms
Road Cam 6-9-2025, 1.08.44pm: 2,839 ms
Road Cam 6-9-2025, 7.55.13am: 8,966 ms
Road Cam 6-7-2025, 2.39.30pm: 11,004 ms
Road Cam 6-9-2025, 9.57.54am: 7,127 ms

70.3 seconds


## Testing instructions for LLM

0. We are going to build and run SpeedDetect across some known sample videos and compare the results we get to the "Target Motion Results" listed above
1. The files we are going to process are all in /src/main/resources/sample_videos/
2. After the full run, let's check the motion results for each one, and compare to the target motion results above. The first motion frame and last motion frame should be within 10 frames of the target results above. If they are, then we can consider that a passing test. If they are not, that is a failed test.
3. A rejected video is OK, we won't call that a fail just a "SKIP"
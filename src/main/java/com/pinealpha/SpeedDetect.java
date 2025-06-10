package com.pinealpha;

import com.pinealpha.model.*;
import com.pinealpha.util.Helper;
import com.pinealpha.util.DatabaseManager;

import java.io.*;
import java.util.*;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgcodecs.Imgcodecs;

public class SpeedDetect {

    public static void main(String[] args) throws IOException, Throwable {
        System.out.println("---- SPEEDCAM STARTING! ----");

        Helper.loadJNIOpenCV();
        DatabaseManager.createTablesIfNotExists();

        Args argsRecord = Helper.parseArgs(args);

        List<String> videoPaths = Helper.getVideoPaths(argsRecord.videoPath());

        int processed = 0;

        for (String videoPath : videoPaths) {
            System.out.println("\nProcessing: " + videoPath);
            MotionResult result = getCarSpeedFromVideo(videoPath, argsRecord.debug());
            result.printMotionResults();
            DatabaseManager.insertMotionResult(result, videoPath);
            processed++;
        }

        System.out.println("---- SPEEDCAM COMPLETE! ----");
    }

    public static MotionResult getCarSpeedFromVideo(String videoPath, boolean debug) throws IOException {
        VideoCapture cap = new VideoCapture(videoPath);

        VideoInfo video = new VideoInfo(
                cap.get(Videoio.CAP_PROP_FPS),
                (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT),
                (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT)
        );
        Helper.printVideoProperties(video);

        // bottom left, bottom right, top right, top left
        List<Point> roadPoints = Arrays.asList(
                new Point(0, 1060),
                new Point(video.frameWidth(), 1935),
                new Point(video.frameWidth(), 800),
                new Point(0, 860)
        );

        MatOfPoint roadPolygon = new MatOfPoint();
        roadPolygon.fromList(roadPoints);
        Mat roiMask = Mat.zeros(video.frameHeight(), video.frameWidth(), org.opencv.core.CvType.CV_8UC1);
        List<MatOfPoint> polygons = Arrays.asList(roadPolygon);
        Imgproc.fillPoly(roiMask, polygons, new Scalar(255));

        // Create background subtractor for motion detection
        var bgSubtractor = Video.createBackgroundSubtractorMOG2();
        bgSubtractor.setDetectShadows(true);
        bgSubtractor.setHistory(10);
        bgSubtractor.setVarThreshold(16); // Lower threshold = more sensitive (default is 16)

        Mat frame = new Mat();
        Mat fgMask = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2));

        int frameCount = 0;
        int firstMotionFrame = -1;
        int lastMotionFrame = -1;
        double firstMotionX = -1; // Track X position of first sustained motion

        // Track consecutive motion frames
        int consecutiveMotionFrames = 0;
        boolean sustainedMotion = false;
        int consecutiveNoMotionFrames = 0;
        boolean carHasPassed = false; // Once a car has passed, don't track new motion

        // Direction detection variables
        boolean directionDetected = false;
        boolean isLeftToRight = true; // default assumption

        // Direction-specific parameters
        double motionThreshold = 0.01;
        double areaThreshold = 2000;
        int consecutiveFramesRequired = 30; // Higher to avoid false starts in problematic videos
        double endMotionThreshold = 0.005; // For detecting when motion ends
        int noMotionFramesBeforeStop = 10; // Consecutive frames with no motion to stop tracking

        // Noise detection variables
        int earlyMotionFrames = 0;
        final int EARLY_FRAME_CUTOFF = 80;
        final double NOISE_THRESHOLD = 0.55; // 55% motion in early frames = too noisy

        while (cap.read(frame)) {
            if (frame.empty()) {
                break;
            }

            // Apply background subtraction to full frame
            bgSubtractor.apply(frame, fgMask);

            // Apply ROI mask to motion mask
            Mat maskedFgMask = new Mat();
            org.opencv.core.Core.bitwise_and(fgMask, roiMask, maskedFgMask);

            // Remove noise with morphological operations
            // Use MORPH_CLOSE to connect nearby regions
            Imgproc.morphologyEx(maskedFgMask, maskedFgMask, Imgproc.MORPH_CLOSE, kernel);

            // Find contours of moving objects in masked area
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(maskedFgMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Filter contours by size (look for car-sized objects)
            int significantContours = 0;
            double totalMotionArea = 0;
            List<MatOfPoint> largeContours = new ArrayList<>();

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);

                if (area > areaThreshold) {
                    significantContours++;
                    totalMotionArea += area;
                    largeContours.add(contour);
                }
            }

            // Find the largest contour area
            double largestContourArea = largeContours.stream()
                    .mapToDouble(c -> Imgproc.contourArea(c))
                    .max()
                    .orElse(0);

            // Calculate motion percentage based on total image area
            double motionPercentage = (totalMotionArea * 100.0) / (video.frameWidth() * video.frameHeight());

            // Basic motion detection (any reasonable motion)
            boolean hasMotion = frameCount > 5 && motionPercentage > motionThreshold && largestContourArea > areaThreshold;

            // Track consecutive motion frames
            if (hasMotion) {
                // Track early motion for noise detection
                if (frameCount < EARLY_FRAME_CUTOFF) {
                    earlyMotionFrames++;
                }
                
                consecutiveMotionFrames++;
                consecutiveNoMotionFrames = 0; // Reset no motion counter
                if (consecutiveMotionFrames >= consecutiveFramesRequired && !sustainedMotion && !carHasPassed) {
                    // We've found significant sustained motion (likely a car)
                    // Only track if no car has passed yet
                    sustainedMotion = true;
                    if (firstMotionFrame == -1) {
                        firstMotionFrame = frameCount - (consecutiveFramesRequired - 1); // Mark when it actually started

                        // Calculate centroid of largest contour to determine initial position
                        if (!largeContours.isEmpty()) {
                            MatOfPoint largestContour = largeContours.stream()
                                    .max((c1, c2) -> Double.compare(Imgproc.contourArea(c1), Imgproc.contourArea(c2)))
                                    .orElse(largeContours.get(0));

                            // Calculate centroid using moments
                            var moments = Imgproc.moments(largestContour);
                            if (moments.m00 != 0) {
                                firstMotionX = moments.m10 / moments.m00;
                            } else {
                                firstMotionX = video.frameWidth() / 2.0; // Default to center if calculation fails
                            }
                            largestContour.release();

                            // Detect direction based on starting position
                            if (!directionDetected) {
                                isLeftToRight = firstMotionX < video.frameWidth() / 2.0;
                                directionDetected = true;
                                System.out.println("Detected direction: " + (isLeftToRight ? "Left-to-Right" : "Right-to-Left"));

                                // Adjust parameters based on direction
                                if (isLeftToRight) {
                                    // Left-to-right parameters
                                    consecutiveFramesRequired = 8; // Even lower for earlier detection
                                    motionThreshold = 0.006; // More sensitive for left-to-right
                                    areaThreshold = 1200; // Lower threshold for smaller cars far away
                                } else {
                                    // Right-to-left: car gets very small as it moves away
                                    endMotionThreshold = 0.002; // Even lower threshold for tiny distant cars
                                    areaThreshold = 800; // Lower to catch small distant cars
                                    noMotionFramesBeforeStop = 25; // Much more tolerance for intermittent detection
                                    motionThreshold = 0.004; // Very sensitive for small motion
                                }
                            }
                        }
                    }
                }
            } else {
                // Reset if no motion detected
                consecutiveMotionFrames = 0;
                consecutiveNoMotionFrames++;

                if (sustainedMotion) {
                    // Stop if motion is very low OR we've had no motion for several frames
                    if (motionPercentage < endMotionThreshold || consecutiveNoMotionFrames >= noMotionFramesBeforeStop) {
                        sustainedMotion = false;
                        carHasPassed = true;
                    }
                }
            }

            if (debug) {
                Helper.writeImageToFile(frame, "target/frame_" + frameCount + (sustainedMotion ? "_sustained" : "") + ".jpg", polygons, largeContours);

                // Also save the motion mask to see what the detector sees
                if (hasMotion || sustainedMotion) {
                    Imgcodecs.imwrite("target/mask_" + frameCount + ".jpg", maskedFgMask);
                }
            }

            if (sustainedMotion && hasMotion) {
                lastMotionFrame = frameCount;
            }

            if (debug) {
                System.out.println("Frame " + frameCount
                        + String.format(": motion=%.4f%%, largest=%.0f, contours=%d, hasMotion=%s, consecutive=%d, sustained=%s",
                                motionPercentage, largestContourArea, significantContours, hasMotion, consecutiveMotionFrames, sustainedMotion));
            } else {
                if (frameCount % 25 == 0) {
                    System.out.println("Frame " + frameCount
                            + String.format(": motion=%.4f%%, largest=%.0f, contours=%d, hasMotion=%s, consecutive=%d",
                                    motionPercentage, largestContourArea, significantContours, hasMotion, consecutiveMotionFrames));
                }
            }

            // Check for excessive noise after early frames
            if (frameCount == EARLY_FRAME_CUTOFF) {
                double earlyMotionRatio = (double) earlyMotionFrames / EARLY_FRAME_CUTOFF;
                if (earlyMotionRatio > NOISE_THRESHOLD) {
                    System.out.println(String.format("Video rejected due to excessive noise: %.1f%% of early frames had motion (threshold: %.1f%%)", 
                        earlyMotionRatio * 100, NOISE_THRESHOLD * 100));
                    return new MotionResult(video, frameCount, -1, -1, -1);
                }
                if (debug) {
                    System.out.println(String.format("Early motion check passed: %.1f%% of frames had motion", earlyMotionRatio * 100));
                }
            }
            
            frameCount++;

            maskedFgMask.release();
            hierarchy.release();
            for (MatOfPoint contour : contours) {
                contour.release();
            }
            contours.clear();
            largeContours.clear();
        }

        // Clean up OpenCV resources
        frame.release();
        fgMask.release();
        kernel.release();
        roiMask.release();
        roadPolygon.release();
        cap.release();

        return new MotionResult(
                video,
                frameCount,
                firstMotionFrame,
                lastMotionFrame,
                firstMotionX
        );
    }

}

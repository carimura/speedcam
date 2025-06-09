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

        MotionResult result = getCarSpeedFromVideo(argsRecord.videoPath(), argsRecord.debug());
        result.printMotionResults();
        DatabaseManager.insertMotionResult(result, argsRecord.videoPath());
        
        System.out.println("---- SPEEDCAM COMPLETE! ----");
    }
    
    public static MotionResult getCarSpeedFromVideo(String videoPath, boolean debug) throws IOException {
        VideoCapture cap = new VideoCapture(Helper.extractResource(videoPath).getAbsolutePath());

        VideoInfo video = new VideoInfo(
                cap.get(Videoio.CAP_PROP_FPS),
                (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT),
                (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT)
        );
        Helper.printVideoProperties(video);

        // bottom left, bottom right, top right, top left
        List<Point> roadPoints = Arrays.asList(
                new Point(45, 1060),
                new Point(video.frameWidth(), 1935),
                new Point(video.frameWidth(), 800),
                new Point(42, 860)
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
        int significantMotionStart;

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
            // Use Imgproc.MORPH_CLOSE to connect nearby regions instead of opening
            Imgproc.morphologyEx(maskedFgMask, maskedFgMask, Imgproc.MORPH_CLOSE, kernel);
            
            // Additional dilation to expand detected regions and connect fragmented parts
            //Imgproc.dilate(maskedFgMask, maskedFgMask, dilateKernel);

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

                // Lower threshold for more sensitivity (was 5000, then 1000, then 500)
                if (area > 2000) {
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
            boolean hasMotion = frameCount > 5 && motionPercentage > 0.01 && largestContourArea > 2000;

            // Track consecutive motion frames
            if (hasMotion) {
                //if (debug) {Helper.writeImageToFile(frame, "target/motion_" + frameCount + (sustainedMotion ? "_sustained" : "") + ".jpg", polygons, largeContours);}
                consecutiveMotionFrames++;
                if (consecutiveMotionFrames >= 20 && !sustainedMotion) {
                    // We've found significant sustained motion (likely a car)
                    sustainedMotion = true;
                    significantMotionStart = frameCount - 19; // Mark when it actually started
                    if (firstMotionFrame == -1) {
                        firstMotionFrame = significantMotionStart;
                        
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
                        }
                    }
                }
            } else {
                // Reset if no motion detected
                consecutiveMotionFrames = 0;
                sustainedMotion = false;
            }

            //if (debug) {Helper.writeImageToFile(frame, "target/frame_" + frameCount + ".jpg", polygons, null);}
            if (debug) {
                Helper.writeImageToFile(frame, "target/frame_" + frameCount + (sustainedMotion ? "_sustained" : "") + ".jpg", polygons, largeContours);
                
                // Also save the motion mask to see what the detector sees
                if (hasMotion || sustainedMotion) {
                    Imgcodecs.imwrite("target/mask_" + frameCount + ".jpg", maskedFgMask);
                }
            }
            
            if (sustainedMotion) {
                lastMotionFrame = frameCount;
            }

            if (debug) {
                System.out.println("Frame " + frameCount
                        + String.format(": motion=%.4f%%, largest=%.0f, contours=%d, hasMotion=%s, consecutive=%d",
                                motionPercentage, largestContourArea, significantContours, hasMotion, consecutiveMotionFrames));
            } else {
                if (frameCount % 25 == 0) { 
                    System.out.println("Frame " + frameCount
                            + String.format(": motion=%.4f%%, largest=%.0f, contours=%d, hasMotion=%s, consecutive=%d",
                                    motionPercentage, largestContourArea, significantContours, hasMotion, consecutiveMotionFrames));
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
        //dilateKernel.release();
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

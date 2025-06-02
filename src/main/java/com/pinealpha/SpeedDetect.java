package com.pinealpha;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.*;
import java.util.*;

record VideoInfo(double fps, int frameWidth, int frameHeight, int totalFrames) {}

public class SpeedDetect {

    public static void main(String[] args) throws IOException, Throwable {
        System.out.println("---- SPEEDCAM STARTING! ----");

        Helper.loadJNIOpenCV();
        
        // Initial test car should be movement from 91 to 198 (107 frames)
        getCarSpeedFromVideo("/test-car.mp4");

        System.out.println("---- SPEEDCAM COMPLETE! ----");
    }


    public static void getCarSpeedFromVideo(String videoPath) throws IOException {
        VideoCapture cap = new VideoCapture(Helper.extractResource(videoPath).getAbsolutePath());
        
        VideoInfo video = new VideoInfo(
            cap.get(Videoio.CAP_PROP_FPS),
            (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH),
            (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT),
            (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT)
        );
        printVideoProperties(video);
        
        // bottom left, bottom right, top right, top left
        List<Point> roadPoints = Arrays.asList(
            new Point(0, 1130),
            new Point(video.frameWidth(), 1975),
            new Point(video.frameWidth(), 800),
            new Point(0, 900)
        );

        MatOfPoint roadPolygon = new MatOfPoint();
        roadPolygon.fromList(roadPoints);
        Mat roiMask = Mat.zeros(video.frameHeight(), video.frameWidth(), org.opencv.core.CvType.CV_8UC1);
        List<MatOfPoint> polygons = Arrays.asList(roadPolygon);
        Imgproc.fillPoly(roiMask, polygons, new Scalar(255));

        // Create background subtractor for motion detection
        var bgSubtractor = Video.createBackgroundSubtractorMOG2();
        bgSubtractor.setDetectShadows(true);
        bgSubtractor.setHistory(10); // Learn background faster
        
        Mat frame = new Mat();
        Mat fgMask = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2)); // Smaller kernel to preserve small motion
        
        int frameCount = 0;
        int framesWithMotion = 0;
        int firstMotionFrame = -1;
        int lastMotionFrame = -1;
        double maxMotionPercentage = 0;
        
        // Track consecutive motion frames
        int consecutiveMotionFrames = 0;
        boolean sustainedMotion = false;
        int significantMotionStart = -1;

        while (cap.read(frame)) {
            if (frame.empty()) break;
            
            // Apply background subtraction to full frame
            bgSubtractor.apply(frame, fgMask);
            
            // Apply ROI mask to motion mask
            Mat maskedFgMask = new Mat();
            org.opencv.core.Core.bitwise_and(fgMask, roiMask, maskedFgMask);
            
            // Remove noise with morphological operations
            Imgproc.morphologyEx(maskedFgMask, maskedFgMask, Imgproc.MORPH_OPEN, kernel);
            
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
                
                // Lower threshold for more sensitivity
                if (area > 1000) { // Reduced from 5000
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
            boolean hasMotion = frameCount > 5 && motionPercentage > 0.01 && largestContourArea > 1000;
            
            // Track consecutive motion frames
            if (hasMotion) {
                consecutiveMotionFrames++;
                if (consecutiveMotionFrames >= 15 && !sustainedMotion) {
                    // We've found significant sustained motion (likely a car)
                    sustainedMotion = true;
                    significantMotionStart = frameCount - 14; // Mark when it actually started
                    if (firstMotionFrame == -1) {
                        firstMotionFrame = significantMotionStart;
                    }
                }
            } else {
                // Reset if no motion detected
                consecutiveMotionFrames = 0;
                sustainedMotion = false;
            }
            
            if (motionPercentage > maxMotionPercentage && frameCount > 5) {
                maxMotionPercentage = motionPercentage;
            }

            // Output all frames
            Mat frameWithROI = frame.clone();
            Imgproc.polylines(frameWithROI, polygons, true, new Scalar(0, 0, 255), 3); // Red polygon for ROI
            Imgcodecs.imwrite("target/frame_" + frameCount + ".jpg", frameWithROI);
            
            if (sustainedMotion) {
                framesWithMotion++;
                lastMotionFrame = frameCount;
                
                //Output all motion frames
                Mat visualization = frame.clone();
                Imgproc.polylines(visualization, polygons, true, new Scalar(0, 0, 255), 3);
                Imgproc.drawContours(visualization, largeContours, -1, new Scalar(0, 255, 0), 2);
                
                Imgcodecs.imwrite("target/motion_" + frameCount + ".jpg", visualization);
                visualization.release(); // Clean up
            }
            
            frameCount++;
            
            // Clean up temporary Mat objects
            maskedFgMask.release();
            hierarchy.release();
            frameWithROI.release();
        }

        System.out.println("\nMotion Detection Results:");
        System.out.println("  Total frames processed: " + frameCount);
        System.out.println("  Frames with significant motion: " + framesWithMotion);
        System.out.println("  Maximum motion percentage: " + String.format("%.2f%%", maxMotionPercentage));
        if (firstMotionFrame != -1) {
            System.out.println("  First motion at frame: " + firstMotionFrame + " (time: " + 
                String.format("%.2f", firstMotionFrame / video.fps()) + "s)");
            System.out.println("  Last motion at frame: " + lastMotionFrame + " (time: " + 
                String.format("%.2f", lastMotionFrame / video.fps()) + "s)");
            System.out.println("  Motion duration: " + (lastMotionFrame - firstMotionFrame) + 
                " frames (" + String.format("%.2f", (lastMotionFrame - firstMotionFrame) / video.fps()) + " seconds)");
        }
        
        // Clean up OpenCV resources
        frame.release();
        fgMask.release();
        kernel.release();
        roiMask.release();
        roadPolygon.release();
        cap.release();
    }

    private static void printVideoProperties(VideoInfo video) {
        System.out.println("Video Properties:");
        System.out.println("  FPS: " + video.fps());
        System.out.println("  Resolution: " + video.frameWidth() + "x" + video.frameHeight());
        System.out.println("  Total Frames: " + video.totalFrames());
        System.out.println("  Duration: " + String.format("%.2f", video.totalFrames() / video.fps()) + " seconds");
    }

}
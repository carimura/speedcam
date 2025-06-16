package com.pinealpha.model;

import org.opencv.core.Point;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for speed detection parameters
 */
public class Config {
    
    /**
     * Get the road polygon points for the given frame width
     * Format: bottom left, bottom right, top right, top left
     */
    public static List<Point> getRoadPoints(int frameWidth) {
        return Arrays.asList(
            new Point(0, 1060),                    // bottom left
            new Point(frameWidth, 1935),           // bottom right
            new Point(frameWidth, 800),            // top right
            new Point(0, 860)                      // top left
        );
    }
    
    // Background subtractor settings
    public static final boolean BG_DETECT_SHADOWS = true;
    public static final int BG_HISTORY = 10;
    public static final double BG_VAR_THRESHOLD = 16.0; // Lower = more sensitive
    
    // Morphological operation kernel size
    public static final int KERNEL_SIZE = 2;
    
    // Frame processing
    public static final int INITIAL_FRAME_SKIP = 5; // Skip first N frames for motion detection
    public static final int FRAME_PROGRESS_INTERVAL = 25; // Print progress every N frames
    
    // Noise detection
    public static final int EARLY_FRAME_CUTOFF = 80;
    public static final double NOISE_THRESHOLD = 0.55; // 55% motion in early frames = too noisy
    
    // Default motion detection parameters (before direction is detected)
    public static final double DEFAULT_MOTION_THRESHOLD = 0.01;
    public static final double DEFAULT_AREA_THRESHOLD = 2000;
    public static final int DEFAULT_CONSECUTIVE_FRAMES_REQUIRED = 20;
    public static final double DEFAULT_END_MOTION_THRESHOLD = 0.005;
    public static final int DEFAULT_NO_MOTION_FRAMES_BEFORE_STOP = 10;
    
    // Direction-specific parameters based on memory
    public static class LeftToRight {
        public static final int CONSECUTIVE_FRAMES_REQUIRED = 8;
        public static final double MOTION_THRESHOLD = 0.007;
        public static final double AREA_THRESHOLD = 1500;
        public static final double END_MOTION_THRESHOLD = 0.005;
        public static final int NO_MOTION_FRAMES_BEFORE_STOP = 10;
    }
    
    public static class RightToLeft {
        public static final int CONSECUTIVE_FRAMES_REQUIRED = 8;
        public static final double MOTION_THRESHOLD = 0.005;
        public static final double AREA_THRESHOLD = 1000;
        public static final double END_MOTION_THRESHOLD = 0.003;
        public static final int NO_MOTION_FRAMES_BEFORE_STOP = 12;
    }
} 
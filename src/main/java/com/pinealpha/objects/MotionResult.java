package com.pinealpha.objects;

public record MotionResult(
    int totalFramesProcessed,
    int firstMotionFrame,
    int lastMotionFrame,
    double fps,  // Store fps for time calculations
    double firstMotionX,  // X position where motion first appeared
    int frameWidth        // Frame width to calculate direction
) {
    private static final double FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT = 155.0;
    private static final double FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT = 165.0;
    private static final double FPS_TO_MPH = 0.681818; // (3600 sec/hr) / (5280 ft/mile)
    
    public boolean hasMotion() {
        return firstMotionFrame != -1;
    }
    
    public String getDirection() {
        if (!hasMotion()) {
            return "Unknown";
        }
        // If motion starts on left half, it's going left to right
        return firstMotionX < frameWidth / 2.0 ? "LeftToRight" : "RightToLeft";
    }
    
    public double getFirstMotionTime() {
        return hasMotion() ? firstMotionFrame / fps : -1;
    }
    
    public double getLastMotionTime() {
        return hasMotion() ? lastMotionFrame / fps : -1;
    }
    
    public int getMotionDurationFrames() {
        return hasMotion() ? lastMotionFrame - firstMotionFrame : 0;
    }
    
    public double getMotionDurationSeconds() {
        return hasMotion() ? getMotionDurationFrames() / fps : 0;
    }
    
    public double getSpeedFeetPerSecond() {
        if (!hasMotion() || getMotionDurationSeconds() == 0) {
            return 0;
        }
        double distance = getDirection().equals("LeftToRight") 
            ? FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT 
            : FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT;
        return distance / getMotionDurationSeconds();
    }
    
    public double getSpeedMph() {
        return getSpeedFeetPerSecond() * FPS_TO_MPH;
    }

    public void printMotionResults() {
        String motionDetails = hasMotion() ? """
                First motion at frame: %d (time: %.2fs)
                Last motion at frame: %d (time: %.2fs)
                Motion Duration: %d frames (%.2f seconds)
                Direction: %s
                Calculated speed: %.1f mph (%.1f ft/s)
              """.formatted(
                firstMotionFrame,
                getFirstMotionTime(),
                lastMotionFrame,
                getLastMotionTime(),
                getMotionDurationFrames(),
                getMotionDurationSeconds(),
                getDirection(),
                getSpeedMph(),
                getSpeedFeetPerSecond()
            ) : "  No significant motion detected";

        double fieldOfViewDistance = hasMotion() && getDirection().equals("LeftToRight") 
            ? FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT 
            : FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT;

        String results = """
            
            Motion Detection Results:
              Total Frames: %d
              Distance: %.0f feet
            %s
            """.formatted(
                totalFramesProcessed,
                fieldOfViewDistance,
                motionDetails
            );
        
        System.out.print(results);
    }
}
package com.pinealpha.model;

public record MotionResult(
    VideoInfo video,
    int totalFramesProcessed,
    int firstMotionFrame,
    int lastMotionFrame,
    double firstMotionX
) {
    private static final double FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT = 155.0;
    private static final double FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT = 165.0;
    
    public boolean hasMotion() {
        return firstMotionFrame != -1;
    }
    
    public String getDirection() {
        if (!hasMotion()) {
            return "Unknown";
        }
        return firstMotionX < video.frameWidth() / 2.0 ? "LeftToRight" : "RightToLeft";
    }
    
    public double getFirstMotionTime() {
        return hasMotion() ? firstMotionFrame / video.fps() : -1;
    }
    
    public double getLastMotionTime() {
        return hasMotion() ? lastMotionFrame / video.fps() : -1;
    }
    
    public int getMotionDurationFrames() {
        return hasMotion() ? lastMotionFrame - firstMotionFrame : 0;
    }
    
    public double getMotionDurationSeconds() {
        return hasMotion() ? getMotionDurationFrames() / video.fps() : 0;
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
        var fps_to_mph = 0.681818;
        return getSpeedFeetPerSecond() * fps_to_mph;
    }

    public void printMotionResults() {
        String motionDetails = hasMotion() ? """
                First motion at frame: %d (time: %.2fs)
                Last motion at frame: %d (time: %.2fs)
                Motion Duration: %d frames (%.2f seconds)
                Direction: %s
                Calculated speed: %.1f mph
              """.formatted(
                firstMotionFrame,
                getFirstMotionTime(),
                lastMotionFrame,
                getLastMotionTime(),
                getMotionDurationFrames(),
                getMotionDurationSeconds(),
                getDirection(),
                getSpeedMph()
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
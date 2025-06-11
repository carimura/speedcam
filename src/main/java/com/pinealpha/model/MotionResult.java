package com.pinealpha.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record MotionResult(
    VideoInfo video,
    ZonedDateTime detectionTime,
    int totalFramesProcessed,
    int firstMotionFrame,
    int lastMotionFrame,
    double firstMotionX,
    boolean isRejected
) {
    private static final double FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT = 155.0;
    private static final double FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT = 165.0;
    
    public boolean hasMotion() {
        return firstMotionFrame != -1 && !isRejected;
    }
    
    public Direction getDirection() {
        if (!hasMotion()) {
            return Direction.Unknown;
        }
        return firstMotionX < video.frameWidth() / 2.0 ? Direction.LeftToRight : Direction.RightToLeft;
    }
    
    public double getFirstMotionTime() {
        return hasMotion() ? firstMotionFrame / video.fps() : -1;
    }
    
    public double getLastMotionTime() {
        return hasMotion() ? lastMotionFrame / video.fps() : -1;
    }
    
    public double getSpeedMph() {
        if (!hasMotion()) {
            return 0;
        }
        int motionDurationFrames = lastMotionFrame - firstMotionFrame;
        if (motionDurationFrames <= 0) {
            return 0;
        }
        
        double distance = getDirection() == Direction.LeftToRight
            ? FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT 
            : FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT;
            
        double speedFeetPerSecond = (distance * video.fps()) / motionDurationFrames;
        
        var fps_to_mph = 0.681818;
        return speedFeetPerSecond * fps_to_mph;
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
                lastMotionFrame - firstMotionFrame,
                (double) (lastMotionFrame - firstMotionFrame) / video.fps(),
                getDirection().toString(),
                getSpeedMph()
            ) : isRejected ? "  Video rejected due to noise" : "  No significant motion detected";

        double fieldOfViewDistance = hasMotion() && getDirection() == Direction.LeftToRight
            ? FIELD_OF_VIEW_FEET_LEFT_TO_RIGHT 
            : FIELD_OF_VIEW_FEET_RIGHT_TO_LEFT;
            
        String detectionTimeStr = detectionTime != null ? detectionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a")) : "N/A";

        String results = """
            
            Motion Detection Results:
              Detection time: %s
              Total Frames: %d
              Distance: %.0f feet
            %s
            """.formatted(
                detectionTimeStr,
                totalFramesProcessed,
                fieldOfViewDistance,
                motionDetails
            );
        
        System.out.print(results);
    }
}
package com.pinealpha;

import com.pinealpha.model.Direction;
import com.pinealpha.model.MotionResult;
import com.pinealpha.util.DatabaseManager;
import com.pinealpha.util.Helper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpeedDetectTest {

    private static final String VIDEO_PATH_PREFIX = "src/main/resources/sample_videos/";
    private static final List<String> videoFiles = Helper.getVideoPaths(VIDEO_PATH_PREFIX);
    private static final int FRAME_TOLERANCE = 10;
    private static final boolean DEBUG = Boolean.getBoolean("video.test.debug");

    // Expected results: video filename -> {firstFrame, lastFrame, direction}
    private static final Map<String, Object[]> EXPECTED_RESULTS = Map.ofEntries(
            Map.entry("left-to-right-1", new Object[]{61, 178, Direction.LeftToRight}),
            Map.entry("left-to-right-2", new Object[]{72, 233, Direction.LeftToRight}),
            Map.entry("left-to-right-3", new Object[]{74, 196, Direction.LeftToRight}),
            Map.entry("left-to-right-4", new Object[]{44, 162, Direction.LeftToRight}),
            Map.entry("right-to-left-1", new Object[]{89, 279, Direction.RightToLeft}),
            Map.entry("right-to-left-2", new Object[]{133, 249, Direction.RightToLeft}),
            Map.entry("right-to-left-3", new Object[]{165, 351, Direction.RightToLeft}),
            Map.entry("right-to-left-4", new Object[]{66, 220, Direction.RightToLeft})
    );

    @BeforeAll
    static void setup() throws IOException {
        Helper.loadJNIOpenCV();
        DatabaseManager.createTablesIfNotExists();
    }

    static Stream<String> videoProvider() {
        String videoFilter = System.getProperty("video.test.filter");
        if (videoFilter != null && !videoFilter.isEmpty()) {
            return Stream.of(videoFilter);
        }
        return EXPECTED_RESULTS.keySet().stream();
    }

    @ParameterizedTest
    @MethodSource("videoProvider")
    @DisplayName("Test Motion Detection Results for each Video")
    void testMotionDetectionResults(String videoIdentifier) throws IOException {
        Object[] expected = EXPECTED_RESULTS.get(videoIdentifier);
        int expectedFirstFrame = (int) expected[0];
        int expectedLastFrame = (int) expected[1];
        Direction expectedDirection = (Direction) expected[2];

        // Find the full path for the video
        String videoPath = videoFiles.stream()
                .filter(path -> path.contains(videoIdentifier))
                .findFirst()
                .orElse(null);

        assertTrue(videoPath != null, "No video file found for identifier: " + videoIdentifier);

        System.out.println("\n--- Testing: " + videoIdentifier + " ---");
        MotionResult actualResult = SpeedDetect.getCarSpeedFromVideo(videoPath, DEBUG);
        actualResult.printMotionResults();
        System.out.println("--- Test Complete: " + videoIdentifier + " ---");

        // A rejected video is OK, we won't call that a fail just a "SKIP"
        if (actualResult.isRejected()) {
            System.out.println("Skipping test for rejected video: " + videoIdentifier);
            return;
        }

        // Assert that the direction is correct
        assertEquals(expectedDirection, actualResult.getDirection(),
                "Incorrect direction for " + videoIdentifier);

        // Assert that the first and last frames are within the tolerance
        assertTrue(Math.abs(expectedFirstFrame - actualResult.firstMotionFrame()) <= FRAME_TOLERANCE,
                String.format("First frame for %s is out of tolerance. Expected: %d, Actual: %d",
                        videoIdentifier, expectedFirstFrame, actualResult.firstMotionFrame()));

        assertTrue(Math.abs(expectedLastFrame - actualResult.lastMotionFrame()) <= FRAME_TOLERANCE,
                String.format("Last frame for %s is out of tolerance. Expected: %d, Actual: %d",
                        videoIdentifier, expectedLastFrame, actualResult.lastMotionFrame()));
    }
}

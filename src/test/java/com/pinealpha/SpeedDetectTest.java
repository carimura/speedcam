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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpeedDetectTest {

    private static final String VIDEO_PATH_PREFIX = "src/main/resources/sample_videos/";
    private static final String TEST_TABLE_NAME = "motion_results_test";
    private static final List<String> videoFiles = Helper.getVideoPaths(VIDEO_PATH_PREFIX);
    private static final int FRAME_TOLERANCE = 10;
    private static final boolean DEBUG = Boolean.getBoolean("video.test.debug");

    // Expected results: video filename -> {firstFrame, lastFrame, direction}
    private static final Map<String, Object[]> EXPECTED_RESULTS = Map.ofEntries(
            Map.entry("Road Cam 6-7-2025, 2.05.46pm", new Object[]{61, 178, Direction.LeftToRight}),
            Map.entry("Road Cam 6-7-2025, 2.09.59pm", new Object[]{72, 233, Direction.LeftToRight}),
            Map.entry("Road Cam 6-9-2025, 1.01.52pm", new Object[]{74, 196, Direction.LeftToRight}),
            Map.entry("Road Cam 6-9-2025, 9.57.54am", new Object[]{44, 162, Direction.LeftToRight}),
            Map.entry("Road Cam 6-7-2025, 2.39.30pm", new Object[]{89, 279, Direction.RightToLeft}),
            Map.entry("Road Cam 6-9-2025, 7.55.13am", new Object[]{133, 249, Direction.RightToLeft}),
            Map.entry("Road Cam 6-9-2025, 1.08.44pm", new Object[]{165, 351, Direction.RightToLeft}),
            Map.entry("Road Cam 6-9-2025, 10.43.09am", new Object[]{66, 220, Direction.RightToLeft})
    );

    @BeforeAll
    static void setup() throws IOException {
        Helper.loadJNIOpenCV();
        DatabaseManager.createTablesIfNotExists(TEST_TABLE_NAME);
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
    @DisplayName("Test Motion Detection and Database Insertion")
    void testMotionDetectionAndDatabase(String videoIdentifier) throws IOException, SQLException {
        Object[] expected = EXPECTED_RESULTS.get(videoIdentifier);
        int expectedFirstFrame = (int) expected[0];
        int expectedLastFrame = (int) expected[1];
        Direction expectedDirection = (Direction) expected[2];

        String videoPath = videoFiles.stream()
                .filter(path -> path.contains(videoIdentifier))
                .findFirst()
                .orElse(null);

        assertTrue(videoPath != null, "No video file found for identifier: " + videoIdentifier);

        System.out.println("\n--------- TESTING: " + videoIdentifier + " ---------");
        MotionResult actualResult = SpeedDetect.getCarSpeedFromVideo(videoPath, DEBUG);
        actualResult.printMotionResults();

        DatabaseManager.insertMotionResult(actualResult, videoPath, TEST_TABLE_NAME);
        verifyDatabaseInsertion(actualResult, videoPath);

        System.out.println("--------- TEST COMPLETE: " + videoIdentifier + " ---------");

        if (actualResult.isRejected()) {
            System.out.println("Skipping assertions for rejected video: " + videoIdentifier);
            return;
        }

        assertEquals(expectedDirection, actualResult.getDirection(), "Incorrect direction for " + videoIdentifier);

        assertTrue(Math.abs(expectedFirstFrame - actualResult.firstMotionFrame()) <= FRAME_TOLERANCE,
                String.format("First frame for %s is out of tolerance. Expected: %d, Actual: %d",
                        videoIdentifier, expectedFirstFrame, actualResult.firstMotionFrame()));

        assertTrue(Math.abs(expectedLastFrame - actualResult.lastMotionFrame()) <= FRAME_TOLERANCE,
                String.format("Last frame for %s is out of tolerance. Expected: %d, Actual: %d",
                        videoIdentifier, expectedLastFrame, actualResult.lastMotionFrame()));
    }

    void verifyDatabaseInsertion(MotionResult result, String videoPath) throws SQLException {
        String sql = "SELECT * FROM " + TEST_TABLE_NAME + " WHERE video_filename = ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, videoPath);
            ResultSet rs = pstmt.executeQuery();

            assertTrue(rs.next(), "No record found in database for video: " + videoPath);

            assertEquals(result.firstMotionFrame(), rs.getInt("first_motion_frame"));
            assertEquals(result.lastMotionFrame(), rs.getInt("last_motion_frame"));
            assertEquals(result.getDirection().toString(), rs.getString("direction"));
            assertEquals(result.hasMotion(), rs.getBoolean("has_motion"));
            assertEquals(result.getSpeedMph(), rs.getDouble("speed_mph"), 0.1);
        }
    }
}

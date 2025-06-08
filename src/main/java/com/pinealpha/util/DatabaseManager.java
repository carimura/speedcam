package com.pinealpha.util;

import com.pinealpha.objects.MotionResult;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres";
    private static final String DB_USER = "postgres.trqkabskuqrnvholggoy";
    private static final String DB_PASSWORD = System.getenv("SUPABASE_PG_SPEEDCAM_PROD");
    
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver not found", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        if (DB_PASSWORD == null || DB_PASSWORD.isEmpty()) {
            throw new IllegalStateException("SUPABASE_PG_SPEEDCAM_PROD environment variable not set");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    public static void createTablesIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS motion_results (
                id SERIAL PRIMARY KEY,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                video_filename VARCHAR(255),
                total_frames_processed INTEGER NOT NULL,
                first_motion_frame INTEGER,
                last_motion_frame INTEGER,
                first_motion_x NUMERIC(8,2),
                has_motion BOOLEAN NOT NULL,
                direction VARCHAR(20),
                first_motion_time NUMERIC(8,2),
                last_motion_time NUMERIC(8,2),
                motion_duration_frames INTEGER,
                motion_duration_seconds NUMERIC(8,2),
                speed_mph NUMERIC(6,2)
            )
            """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("motion_results table created or already exists");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void insertMotionResult(MotionResult result, String videoFilename) {
        String insertSQL = """
            INSERT INTO motion_results (
                video_filename, total_frames_processed, first_motion_frame, 
                last_motion_frame, first_motion_x, has_motion,
                direction, first_motion_time, last_motion_time, motion_duration_frames,
                motion_duration_seconds, speed_mph
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setString(1, videoFilename);
            pstmt.setInt(2, result.totalFramesProcessed());
            pstmt.setInt(3, result.firstMotionFrame());
            pstmt.setInt(4, result.lastMotionFrame());
            pstmt.setDouble(5, result.firstMotionX());
            pstmt.setBoolean(6, result.hasMotion());
            pstmt.setString(7, result.getDirection());
            pstmt.setDouble(8, result.getFirstMotionTime());
            pstmt.setDouble(9, result.getLastMotionTime());
            pstmt.setInt(10, result.getMotionDurationFrames());
            pstmt.setDouble(11, result.getMotionDurationSeconds());
            pstmt.setDouble(12, result.getSpeedMph());
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Motion result saved to database (" + rowsAffected + " row inserted)");
            
        } catch (SQLException e) {
            System.err.println("Error inserting motion result: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("Connected to database: " + metaData.getDatabaseProductName());
            System.out.println("Database version: " + metaData.getDatabaseProductVersion());
            System.out.println("Connection successful!");
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
package com.pinealpha.util;

import com.pinealpha.model.MotionResult;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres";
    private static final String DB_USER = "postgres.trqkabskuqrnvholggoy";
    private static final String DB_NAME = "motion_results_prod";
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
    
    private static String getTableName(String... tableName) {
        return (tableName.length > 0 && tableName[0] != null) ? tableName[0] : DB_NAME;
    }

    public static void createTablesIfNotExists(String... tableName) {
        String actualTableName = getTableName(tableName);
        String createTableSQL = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id SERIAL PRIMARY KEY,
                detection_time TIMESTAMP,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                video_filename VARCHAR(255),
                first_motion_frame INTEGER,
                last_motion_frame INTEGER,
                has_motion BOOLEAN NOT NULL,
                direction VARCHAR(20),
                first_motion_time NUMERIC(8,2),
                last_motion_time NUMERIC(8,2),
                speed_mph NUMERIC(6,2)
            )
            """, actualTableName);
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println(actualTableName + " table created or already exists");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void insertMotionResult(MotionResult result, String videoFilename, String... tableName) {
        String actualTableName = getTableName(tableName);
        String insertSQL = String.format("""
            INSERT INTO %s (
                detection_time, video_filename, first_motion_frame, 
                last_motion_frame, has_motion,
                direction, first_motion_time, last_motion_time, speed_mph
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, actualTableName);
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setTimestamp(1, Timestamp.from(result.detectionTime().toInstant()));
            pstmt.setString(2, videoFilename);
            pstmt.setInt(3, result.firstMotionFrame());
            pstmt.setInt(4, result.lastMotionFrame());
            pstmt.setBoolean(5, result.hasMotion());
            pstmt.setString(6, result.getDirection().toString());
            pstmt.setDouble(7, result.getFirstMotionTime());
            pstmt.setDouble(8, result.getLastMotionTime());
            pstmt.setDouble(9, result.getSpeedMph());
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Motion result saved to " + actualTableName + " (" + rowsAffected + " row inserted)");
            
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
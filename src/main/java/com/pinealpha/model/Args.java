package com.pinealpha.model;

public record Args(String videoPath, boolean debug) {
    
    public static void printUsage() {
        System.out.println("\nUsage: java -jar speedcam.jar --in <video_path_or_directory> [--debug]");
        System.out.println("\nRequired arguments:");
        System.out.println("  --in <path>   Path to a video file or directory containing videos (local file system only)");
        System.out.println("\nOptional arguments:");
        System.out.println("  --debug               Enable debug mode (outputs frame images)");
        System.out.println("  --help, -h            Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  java -jar speedcam.jar --in /path/to/video.mp4 --debug");
        System.out.println("  java -jar speedcam.jar --in /path/to/videos/");
    }
}
package com.pinealpha.objects;

public class Args {
    public String videoPath;
    public boolean debug;

    public void printUsage() {
        System.out.println("\nUsage: java -jar speedcam.jar --in <video_path> [--debug]");
        System.out.println("\nRequired arguments:");
        System.out.println("  --in <path>   Path to the video file to analyze");
        System.out.println("\nOptional arguments:");
        System.out.println("  --debug               Enable debug mode (outputs frame images)");
        System.out.println("  --help, -h            Show this help message");
        System.out.println("\nExample:");
        System.out.println("  java -jar speedcam.jar --in /sample_videos/left_to_right_1.mp4 --debug");
    }
}
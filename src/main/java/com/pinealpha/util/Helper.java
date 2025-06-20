package com.pinealpha.util;

import com.pinealpha.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.MatOfPoint;
import java.util.ArrayList;

public class Helper {

    public static Args parseArgs(String[] args) {
        String videoPath = null;
        boolean debug = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--in" -> {
                    if (i + 1 < args.length) {
                        videoPath = args[++i];
                    } else {
                        System.err.println("Error: --in requires a video path argument");
                        Args.printUsage();
                        System.exit(1);
                    }
                }
                case "--debug" -> debug = true;
                case "--help", "-h" -> {
                    Args.printUsage();
                    System.exit(0);
                }
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    Args.printUsage();
                    System.exit(1);
                }
            }
        }
        
        // Validate required arguments
        if (videoPath == null) {
            System.err.println("Error: Video path is required");
            Args.printUsage();
            System.exit(1);
        }

        System.out.println("Processing: " + videoPath);
        if (debug) {
            System.out.println("Debug mode: ENABLED");
        }

        return new Args(videoPath, debug);
    }

    public static void writeImageToFile(Mat frame, String filename, List<MatOfPoint> polygons, List<MatOfPoint> largeContours) {
        Mat frameWithROI = frame.clone();

        if (polygons != null) {
            Imgproc.polylines(frameWithROI, polygons, true, new Scalar(0, 0, 255), 3);
        }
        
        if (largeContours != null) {
            Imgproc.drawContours(frameWithROI, largeContours, -1, new Scalar(0, 255, 0), 2);
        }
        
        Imgcodecs.imwrite(filename, frameWithROI);
        frameWithROI.release();
    }

    public static void printVideoProperties(VideoInfo video) {
        System.out.println("Video Properties:");
        System.out.println("  FPS: " + video.fps());
        System.out.println("  Resolution: " + video.frameWidth() + "x" + video.frameHeight());
        System.out.println("  Total Frames: " + video.totalFrames());
        System.out.println("  Duration: " + String.format("%.2f", video.totalFrames() / video.fps()) + " seconds");
    }


    public static File extractResource(String resource) throws IOException {
        try (InputStream in = Helper.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new FileNotFoundException(resource);
            }

            File fileOut = new File(System.getProperty("java.io.tmpdir"), "tempFile" + System.currentTimeMillis());
            fileOut.deleteOnExit();

            try (OutputStream out = new FileOutputStream(fileOut)) {
                in.transferTo(out);
            }
            return fileOut;
        }

    }

    public static void extractLibrary(String resourcePath, Path destination) throws IOException {
        try (InputStream in = Helper.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Missing resource: " + resourcePath);
            }
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            destination.toFile().deleteOnExit();
            destination.toFile().setExecutable(true, false);
        }
    }

    public static void loadNativeOpenCV() throws Throwable {
        List<String> libraries = List.of(
                "libopencv_core.412.dylib",
                "libopencv_imgcodecs.412.dylib",
                "libopencv_objdetect.412.dylib",
                "libopencv_imgproc.412.dylib",
                "libcvwrapper.dylib"
        );

        // Extract all native libraries to temp dir
        Path tempDir = Files.createTempDirectory("native-libs");
        tempDir.toFile().deleteOnExit();
        for (String lib : libraries) {
            Helper.extractLibrary("native/" + lib, tempDir.resolve(lib));
        }

        // Add extracted dir to library path for OpenCV dependencies
        System.setProperty("java.library.path", tempDir.toString());
        System.load(tempDir.resolve("libcvwrapper.dylib").toString());
    }

    public static void loadJNIOpenCV() throws IOException {
        System.load(extractResource("/native/libopencv_java4120.dylib").getAbsolutePath());
    }

    public static List<String> getVideoPaths(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("Error: Path does not exist: " + path);
            System.exit(1);
        }
        
        if (file.isDirectory()) {
            return getVideoFiles(path);
        } else {
            return List.of(path);
        }
    }

    public static List<String> getVideoFiles(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.isDirectory()) {
            return List.of(directoryPath);
        }
        
        // Common video file extensions
        String[] videoExtensions = {".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm"};
        
        File[] files = dir.listFiles((dir1, name) -> {
            String lowerName = name.toLowerCase();
            return Arrays.stream(videoExtensions).anyMatch(lowerName::endsWith);
        });
        
        if (files == null || files.length == 0) {
            System.out.println("No video files found in directory: " + directoryPath);
            return List.of();
        }
        
        // Sort files for consistent processing order
        Arrays.sort(files);
        
        System.out.println("Found " + files.length + " video files to process:");
        List<String> videoPaths = new ArrayList<>();
        for (File file : files) {
            System.out.println("  - " + file.getName());
            videoPaths.add(file.getAbsolutePath());
        }
        
        return videoPaths;
    }

    public static ZonedDateTime parseDateTimeFromFilename(String fileName) {
        var str = fileName.replace("Road Cam ", "");
        str = str.split(" - ")[0];
        str = str.replace("PDT", "");
        str = str.replace("am", "AM");
        str = str.replace("pm", "PM").trim();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M-d-uuuu, h.m.sa");
        LocalDateTime localDateTime = LocalDateTime.parse(str, formatter);
        return localDateTime.atZone(java.time.ZoneId.of("America/Los_Angeles"));
    }
}

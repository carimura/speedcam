package com.pinealpha;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.*;

import java.lang.foreign.*;
import java.lang.invoke.*;


public class Main {

    void main(String[] args) throws IOException, Throwable {
        System.out.println("---- SPEEDCAM STARTING! ----");
        //Helper.loadJNIOpenCV();

        Helper.loadNativeOpenCV();

        //faceDetect("/astrid.png", "target/output.png");
        faceDetectPanama("/astrid.png", "target/output.png");


        //videoProcess("/output_clean.mp4", "target/output.mp4");

        System.out.println("---- PROCESSING COMPLETE! ----");
    }



    public void faceDetect(String resource, String outFile) throws IOException {
        Mat imgIn = Imgcodecs.imread(Helper.extractResource(resource).getAbsolutePath());

        CascadeClassifier faceDetector = new CascadeClassifier(Helper.extractResource("/haarcascade_frontalface_alt.xml").getAbsolutePath());
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(imgIn, faces);
        for (Rect rect : faces.toArray()) {
            Imgproc.rectangle(imgIn, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 5);
        }
        Imgcodecs.imwrite(outFile, imgIn);
    }


    public void faceDetectPanama(String in, String out) throws IOException, Throwable {
        var inImg = Helper.extractResource(in).getAbsolutePath();
        var cascadePath = Helper.extractResource("/haarcascade_frontalface_alt.xml").getAbsolutePath();

        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            Linker linker = Linker.nativeLinker();

            MethodHandle faceDetect = linker.downcallHandle(
                    lookup.find("face_detect").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            MemorySegment inMs = arena.allocateFrom(inImg);
            MemorySegment cascade = arena.allocateFrom(cascadePath);
            MemorySegment outMs = arena.allocateFrom(out);

            int result = (int) faceDetect.invoke(inMs, cascade, outMs);

            System.out.println("Face detection " + (result == 1 ? "succeeded" : "failed"));
        }
    }


    public void videoProcess(String resource, String outFile) throws IOException {
        VideoCapture cap = new VideoCapture(Helper.extractResource(resource).getAbsolutePath());
        if (!cap.isOpened()) {
            System.out.println("Error: Cannot open video file.");
            return;
        }

        CascadeClassifier faceDetector = new CascadeClassifier(Helper.extractResource("/haarcascade_frontalface_alt.xml").getAbsolutePath());

        Mat frame = new Mat();
        int frameCount = 0;

        while (cap.read(frame)) {
            if (frame.empty()) break;
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(frame, faces);
            for (Rect face : faces.toArray()) {
                Imgproc.rectangle(frame, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
            }
            // Optional: Save every nth frame for debugging
            if (frameCount % 2 == 0) {
                Imgcodecs.imwrite("target/frame_" + frameCount + ".jpg", frame);
            }
            frameCount++;
        }

        cap.release();
    }

}
package com.pinealpha;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.*;

import java.lang.foreign.*;
import java.lang.invoke.*;


public class FaceDetect {

    public static void main(String[] args) throws IOException, Throwable {
        System.out.println("---- FACE DETECT STARTING! ----");
        //Helper.loadJNIOpenCV();
        //faceDetect("/chad.png", "target/output.png");

        Helper.loadNativeOpenCV();
        faceDetectPanama("/chad.png", "target/output.png");

        System.out.println("---- FACE DETECT COMPLETE! ----");
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


    public static void faceDetectPanama(String in, String out) throws IOException, Throwable {
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

    public void testPanama() throws IOException, Throwable {
        System.out.println("------ Panama ------");

        Helper.loadNativeOpenCV();

        String imagePath = Helper.extractResource("/astrid.png").getAbsolutePath();

        var linker = Linker.nativeLinker();

        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup lookup = SymbolLookup.loaderLookup();

            MethodHandle getImageWidth = linker.downcallHandle(
                lookup.find("get_image_width").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );

            MemorySegment utf8Path = arena.allocateFrom(imagePath);

            int width = (int) getImageWidth.invoke(utf8Path);
            System.out.println("Image width: " + width);
        }
        System.out.println("------ DONE ------");
    }


}
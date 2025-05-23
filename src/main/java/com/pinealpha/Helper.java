package com.pinealpha;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class Helper {
    public static File extractResource(String resource) throws IOException {
        try (InputStream in = Helper.class.getResourceAsStream(resource)) {
            if (in == null) throw new FileNotFoundException(resource);

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
            if (in == null) throw new FileNotFoundException("Missing resource: " + resourcePath);
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
}

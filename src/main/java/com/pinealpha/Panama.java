package com.pinealpha;

import java.lang.foreign.*;
import java.lang.invoke.*;

public class Panama {
    public static void main(String[] args) throws Throwable {
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


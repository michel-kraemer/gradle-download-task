package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.file.RegularFile;

import java.io.File;

public class RegularFileGet {
    public static boolean isRegularFile(Object destObject) {
        return destObject instanceof RegularFile;
    }

    public static File getRegularFile(Object destObject) {
        File cachedDest = ((RegularFile) destObject).getAsFile();
        return cachedDest;
    }
}

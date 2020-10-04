package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.file.Directory;

import java.io.File;

public class DirectoryGet {
    public static boolean isDirectory(Object destObject) {
        return destObject instanceof Directory;
    }

    public static File getDirectory(Object destObject) {
        File cachedDest = ((Directory) destObject).getAsFile();
        cachedDest.mkdirs();
        return cachedDest;
    }
}

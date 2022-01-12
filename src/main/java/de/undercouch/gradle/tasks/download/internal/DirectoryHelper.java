package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.file.Directory;
import org.gradle.util.GradleVersion;

import java.io.File;

/**
 * Helper methods to access {@link Directory}
 * @author Michel Kraemer
 */
public class DirectoryHelper {
    /**
     * Check if the given object is a {@link Directory}
     * @param obj the object
     * @return {@code true} if {@code obj} is a {@link Directory}
     */
    public static boolean isDirectory(Object obj) {
        return GradleVersion.current().compareTo(GradleVersion.version("4.1")) > 0 &&
                obj instanceof Directory;
    }

    /**
     * Convert the given {@link Directory} object to a {@link File}
     * @param obj the {@link Directory}
     * @return the {@link File}
     */
    public static File getFileFromDirectory(Object obj) {
        return ((Directory)obj).getAsFile();
    }
}

package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.file.RegularFile;
import org.gradle.util.GradleVersion;

import java.io.File;

/**
 * Helper methods to access {@link RegularFile}
 * @author Michel Kraemer
 */
public class RegularFileHelper {
    /**
     * Check if the given object is a {@link RegularFile}
     * @param obj the object
     * @return {@code true} if {@code obj} is a {@link RegularFile}
     */
    public static boolean isRegularFile(Object obj) {
        return GradleVersion.current().compareTo(GradleVersion.version("4.1")) > 0 &&
                obj instanceof RegularFile;
    }

    /**
     * Convert the given {@link RegularFile} object to a {@link File}
     * @param obj the {@link RegularFile} object
     * @return the {@link File}
     */
    public static File getFileFromRegularFile(Object obj) {
        return ((RegularFile)obj).getAsFile();
    }
}

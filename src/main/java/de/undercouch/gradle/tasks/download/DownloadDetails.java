package de.undercouch.gradle.tasks.download;

import org.gradle.api.file.RelativePath;

import java.net.URL;

/**
 * Provides details about a download source and its target file
 * @author Michel Kraemer
 */
public interface DownloadDetails {
    /**
     * Set the name of the target file
     * @param name the filename
     */
    void setName(String name);

    /**
     * Get the name of the target file
     * @return the filename
     */
    String getName();

    /**
     * Get the source URL
     * @return the URL
     */
    URL getSourceURL();

    /**
     * Get the path of the target file, relative to download directory
     * @return the path of the target file, relative to download directory
     */
    RelativePath getRelativePath();

    /**
     * Set the path of the target file (including the filename)
     * @param path the path of the target file (including the filename)
     */
    void setRelativePath(RelativePath path);

    /**
     * Get the path of the target file, relative to download directory
     * @return the path of the target file, relative to download directory
     */
    String getPath();

    /**
     * Set the path of the target file (including the filename)
     * @param path the path of the target file (including the filename)
     */
    void setPath(String path);
}

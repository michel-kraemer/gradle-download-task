package de.undercouch.gradle.tasks.download;

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
}

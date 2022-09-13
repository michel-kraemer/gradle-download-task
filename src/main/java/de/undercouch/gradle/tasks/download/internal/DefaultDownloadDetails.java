package de.undercouch.gradle.tasks.download.internal;

import de.undercouch.gradle.tasks.download.DownloadDetails;

import java.net.URL;
import java.util.Objects;

/**
 * Default implementation of {@link DownloadDetails}
 * @author Michel Kraemer
 */
public class DefaultDownloadDetails implements DownloadDetails {
    private String name;
    private final URL sourceURL;

    public DefaultDownloadDetails(String name, URL sourceURL) {
        this.name = name;
        this.sourceURL = sourceURL;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public URL getSourceURL() {
        return this.sourceURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultDownloadDetails that = (DefaultDownloadDetails)o;
        return name.equals(that.name) && sourceURL.equals(that.sourceURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sourceURL);
    }

    @Override
    public String toString() {
        return "DefaultDownloadDetails{" +
                "name='" + name + '\'' +
                ", sourceURL=" + sourceURL +
                '}';
    }
}

package de.undercouch.gradle.tasks.download.internal;

import de.undercouch.gradle.tasks.download.DownloadDetails;
import org.gradle.api.file.RelativePath;

import java.net.URL;
import java.util.Objects;

/**
 * Default implementation of {@link DownloadDetails}
 * @author Michel Kraemer
 */
public class DefaultDownloadDetails implements DownloadDetails {
    private RelativePath relativePath;
    private final URL sourceURL;

    public DefaultDownloadDetails(RelativePath relativePath, URL sourceURL) {
        this.relativePath = relativePath;
        this.sourceURL = sourceURL;
    }

    @Override
    public void setName(String name) {
        relativePath = relativePath.replaceLastName(name);
    }

    @Override
    public String getName() {
        return relativePath.getLastName();
    }

    @Override
    public URL getSourceURL() {
        return this.sourceURL;
    }

    @Override
    public RelativePath getRelativePath() {
        return relativePath;
    }

    @Override
    public void setRelativePath(RelativePath path) {
        relativePath = path;
    }

    @Override
    public String getPath() {
        return relativePath.getPathString();
    }

    @Override
    public void setPath(String path) {
        relativePath = RelativePath.parse(relativePath.isFile(), path);
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
        return relativePath.equals(that.relativePath) &&
                sourceURL.toString().equals(that.sourceURL.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, sourceURL);
    }

    @Override
    public String toString() {
        return "DefaultDownloadDetails{" +
                "relativePath='" + relativePath + '\'' +
                ", sourceURL=" + sourceURL +
                '}';
    }
}

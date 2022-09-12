package de.undercouch.gradle.tasks.download;

import org.gradle.api.Action;
import org.gradle.api.Project;

import java.util.concurrent.CompletableFuture;

/**
 * An extension that executes a {@link DownloadAction}
 * @author Michel Kraemer
 */
public class DownloadExtension {
    private final Project project;
    
    /**
     * Creates a new extension
     * @param project the project to be built
     */
    public DownloadExtension(Project project) {
        this.project = project;
    }

    /**
     * Download a file now
     * @param action action that configures a given {@code DownloadSpec}
     */
    public void run(Action<DownloadSpec> action) {
        DownloadAction da = new DownloadAction(project);
        action.execute(da);
        try {
            da.execute(false).get();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "Could not download file";
            }
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Download a file asynchronously
     * @param action action that configures a given {@code DownloadSpec}
     * @return a {@link CompletableFuture} that completes successfully when
     * the download has finished successfully or that completes exceptionally
     * if the download has failed.
     */
    public CompletableFuture<Void> runAsync(Action<DownloadSpec> action) {
        DownloadAction da = new DownloadAction(project);
        action.execute(da);
        try {
            return da.execute(false);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "Could not download file";
            }
            throw new IllegalStateException(message, e);
        }
    }
}

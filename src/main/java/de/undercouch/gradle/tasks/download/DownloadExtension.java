package de.undercouch.gradle.tasks.download;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * An extension that executes a {@link DownloadAction}
 * @author Michel Kraemer
 */
public class DownloadExtension {
    private final ProjectLayout projectLayout;
    private final Logger logger;
    private final Object servicesOwner;
    private final ObjectFactory objectFactory;
    private final boolean isOffline;
    private final File buildDir;
    
    /**
     * Creates a new extension
     * @param project the project to be built
     */
    public DownloadExtension(Project project) {
        this(project, null);
    }

    /**
     * Creates a new extension
     * @param task the current task
     */
    @Inject
    public DownloadExtension(Task task) {
        this(task.getProject(), task);
    }

    /**
     * Creates a new extension
     * @param project the project to be built
     * @param task the current task
     */
    public DownloadExtension(Project project, @Nullable Task task) {
        this(project.getLayout(), project.getLogger(),
                task != null ? task : project, project.getObjects(),
                project.getGradle().getStartParameter().isOffline(),
                project.getLayout().getBuildDirectory().getAsFile().get());
    }

    /**
     * Creates a new extension
     * @param projectLayout the project layout
     * @param logger the project logger
     * @param servicesOwner either the current project or (preferably) the
     * current task
     * @param objectFactory the project's object factory
     * @param isOffline whether Gradle has been started in offline mode or not
     * @param buildDir the project's build directory
     */
    private DownloadExtension(ProjectLayout projectLayout, Logger logger,
            Object servicesOwner, ObjectFactory objectFactory, boolean isOffline,
            File buildDir) {
        this.projectLayout = projectLayout;
        this.logger = logger;
        this.servicesOwner = servicesOwner;
        this.objectFactory = objectFactory;
        this.isOffline = isOffline;
        this.buildDir = buildDir;
    }

    /**
     * Download a file now
     * @param action action that configures a given {@code DownloadSpec}
     */
    public void run(Action<DownloadSpec> action) {
        DownloadAction da = new DownloadAction(projectLayout, logger,
                servicesOwner, objectFactory, isOffline, buildDir);
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
        DownloadAction da = new DownloadAction(projectLayout, logger,
                servicesOwner, objectFactory, isOffline, buildDir);
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

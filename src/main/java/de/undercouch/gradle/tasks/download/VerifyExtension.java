package de.undercouch.gradle.tasks.download;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ProjectLayout;

import javax.inject.Inject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * An extension that executes a {@link VerifyAction}
 * @author Michel Kraemer
 */
public class VerifyExtension {
    private final ProjectLayout projectLayout;

    /**
     * Creates a new extension
     * @param project the project to be built
     */
    public VerifyExtension(Project project) {
        this.projectLayout = project.getLayout();
    }

    /**
     * Creates a new extension
     * @param task the current task
     */
    @Inject
    public VerifyExtension(Task task) {
        this(task.getProject());
    }

    public void run(Action<VerifySpec> action) {
        VerifyAction va = new VerifyAction(projectLayout);
        action.execute(va);
        try {
            va.execute();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not verify file checksum", e);
        }
    }
}

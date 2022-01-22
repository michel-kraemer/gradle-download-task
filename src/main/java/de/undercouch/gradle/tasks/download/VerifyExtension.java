package de.undercouch.gradle.tasks.download;

import org.gradle.api.Action;
import org.gradle.api.Project;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * An extension that executes a {@link VerifyAction}
 * @author Michel Kraemer
 */
public class VerifyExtension {
    private final Project project;

    /**
     * Creates a new extension
     * @param project the project to be built
     */
    public VerifyExtension(Project project) {
        this.project = project;
    }

    public void run(Action<VerifySpec> action) {
        VerifyAction va = new VerifyAction(project);
        action.execute(va);
        try {
            va.execute();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not verify file checksum", e);
        }
    }
}

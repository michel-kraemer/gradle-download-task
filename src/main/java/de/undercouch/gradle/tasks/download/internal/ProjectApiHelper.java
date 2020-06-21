package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.util.GradleVersion;

import javax.inject.Inject;
import java.io.File;

public abstract class ProjectApiHelper {

    public static ProjectApiHelper newInstance(Project project) {
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.3")) >= 0) {
            return project.getObjects().newInstance(DefaultProjectApiHelper.class);
        }
        return new LegacyProjectApiHelper(project);
    }

    public abstract File getBuildDirectory();

    public abstract File file(String path);
}

class DefaultProjectApiHelper extends ProjectApiHelper {

    private final ProjectLayout layout;

    @Inject
    public DefaultProjectApiHelper(ProjectLayout layout) {
        this.layout = layout;
    }

    @Override
    public File getBuildDirectory() {
        return layout.getBuildDirectory().get().getAsFile();
    }

    @Override
    public File file(String path) {
        return layout.getProjectDirectory().file(path).getAsFile();
    }
}

class LegacyProjectApiHelper extends ProjectApiHelper {

    private final Project project;

    public LegacyProjectApiHelper(Project project) {
        this.project = project;
    }

    @Override
    public File getBuildDirectory() {
        return project.getBuildDir();
    }

    @Override
    public File file(String path) {
        return project.file(path);
    }
}

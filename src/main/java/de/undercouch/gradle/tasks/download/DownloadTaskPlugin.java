package de.undercouch.gradle.tasks.download;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

/**
 * Registers the extensions provided by this plugin
 * @author Michel Kraemer
 */
public class DownloadTaskPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("download", DownloadExtension.class, project);
        if (project.getExtensions().findByName("verifyChecksum") == null) {
            project.getExtensions().create("verifyChecksum", VerifyExtension.class, project);
        }

        // register top-level properties 'Download' and 'Verify' for our tasks
        ExtraPropertiesExtension extraProperties =
                project.getExtensions().getExtraProperties();
        extraProperties.set(Download.class.getSimpleName(), Download.class);
        extraProperties.set(Verify.class.getSimpleName(), Verify.class);
    }
}

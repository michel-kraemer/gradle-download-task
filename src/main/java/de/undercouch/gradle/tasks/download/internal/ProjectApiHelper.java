// Copyright 2013-2020 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

import java.io.File;

/**
 * Provides unified access to the API of the {@link Project} for different
 * Gradle versions.
 * @author Paul Merlin
 */
public abstract class ProjectApiHelper {
    /**
     * Creates a new instance of the {@link ProjectApiHelper} for the given
     * {@link Project} depending on the Gradle version
     * @param project the project to create a helper for
     * @return the helper
     */
    public static ProjectApiHelper newInstance(Project project) {
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.3")) >= 0) {
            return project.getObjects().newInstance(DefaultProjectApiHelper.class);
        }
        return new LegacyProjectApiHelper(project);
    }

    /**
     * @return the project's build directory
     */
    public abstract File getBuildDirectory();

    /**
     * Resolves a file path relative to the project directory of this project
     * @param path the path
     * @return the file
     */
    public abstract File file(String path);
}

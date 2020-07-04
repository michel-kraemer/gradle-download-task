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

import java.io.File;

/**
 * Provides access to the {@link Project} API for Gradle 4.2 and lower
 * @author Paul Merlin
 */
class LegacyProjectApiHelper extends ProjectApiHelper {
    private final Project project;

    /**
     * Create the helper
     * @param project the project to wrap
     */
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

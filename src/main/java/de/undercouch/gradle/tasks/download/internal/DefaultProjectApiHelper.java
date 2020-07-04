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
import org.gradle.api.file.ProjectLayout;

import javax.inject.Inject;
import java.io.File;

/**
 * Provides access to the {@link Project} API for Gradle 4.3 and later
 * @author Paul Merlin
 */
class DefaultProjectApiHelper extends ProjectApiHelper {
    private final ProjectLayout layout;

    /**
     * Create the API helper
     * @param layout the project's layout
     */
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

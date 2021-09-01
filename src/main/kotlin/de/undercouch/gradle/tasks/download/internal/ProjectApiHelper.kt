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
package de.undercouch.gradle.tasks.download.internal

import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.io.File

/**
 * Provides unified access to the API of the [Project] for different
 * Gradle versions.
 * @author Paul Merlin
 */
abstract class ProjectApiHelper {
    /**
     * @return the project's build directory
     */
    abstract val buildDirectory: File

    /**
     * Resolves a file path relative to the project directory of this project
     * @param path the path
     * @return the file
     */
    abstract fun file(path: String): File

    companion object {
        /**
         * Creates a new instance of the [ProjectApiHelper] for the given
         * [Project] depending on the Gradle version
         * @param project the project to create a helper for
         * @return the helper
         */
        fun newInstance(project: Project): ProjectApiHelper = when {
            GradleVersion.current().baseVersion >= GradleVersion.version("4.3") ->
                project.objects.newInstance(DefaultProjectApiHelper::class.java)
            else -> LegacyProjectApiHelper(project)
        }
    }
}
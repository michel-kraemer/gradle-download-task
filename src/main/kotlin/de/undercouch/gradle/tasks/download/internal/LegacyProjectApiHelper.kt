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

import de.undercouch.gradle.tasks.download.internal.ProjectApiHelper
import org.gradle.api.Project
import java.io.File

/**
 * Provides access to the [Project] API for Gradle 4.2 and lower
 * @author Paul Merlin
 */
internal class LegacyProjectApiHelper
/**
 * Create the helper
 * @param project the project to wrap
 */
    (private val project: Project) : ProjectApiHelper() {

    override val buildDirectory: File
        get() = project.buildDir

    override fun file(path: String): File = project.file(path)
}
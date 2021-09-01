// Copyright 2013 Michel Kraemer
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
package de.undercouch.gradle.tasks.download

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.Configurable
import org.gradle.util.ConfigureUtil
import java.io.IOException

/**
 * An extension that configures and executes a [DownloadAction]
 * @author Michel Kraemer
 */
class DownloadExtension
/**
 * Creates a new extension
 * @param project the project to be built
 */
    (private val project: Project) : Configurable<DownloadExtension> {

    override fun configure(cl: Closure<*>?): DownloadExtension {
        val da = ConfigureUtil.configure(cl, DownloadAction(project))
        try {
            da.execute()
        } catch (e: IOException) {
            var message = e.message
            if (message == null) {
                message = "Could not download file"
            }
            throw IllegalStateException(message, e)
        }
        return this
    }
}
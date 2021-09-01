// Copyright 2013-2019 Michel Kraemer
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

import de.undercouch.gradle.tasks.download.internal.ms
import org.gradle.api.DefaultTask
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException

/**
 * Downloads a file and displays progress. Example:
 * <pre>
 * task downloadFile(type: Download) {
 * src 'http://www.example.com/file.ext'
 * dest buildDir
 * }
</pre> *
 * @author Michel Kraemer
 */
class Download : DefaultTask(), DownloadSpec {

    private val action: DownloadAction = DownloadAction(project, this)

    /**
     * Starts downloading
     * @throws IOException if the file could not be downloaded
     */
    @TaskAction
    @Throws(IOException::class)
    fun download() {
        action.execute()

        // handle 'upToDate'
        try {
            if (action.isUpToDate()) {
                val state = this.javaClass.getMethod("getState")(this)
                try {
                    // prior to Gradle 3.2 we needed to do this
                    state.javaClass.getMethod("upToDate")(state)
                } catch (e: NoSuchMethodException) {
                    // since Gradle 3.2 we need to do this
                    state.javaClass.getMethod("setDidWork", Boolean::class.javaPrimitiveType)(state, false)
                }
            }
        } catch (e: Exception) {
            //just ignore
        }
    }

    /**
     * Default constructor
     */
    init {
        // get required project properties now to enable configuration cache
        val isOffline = project.gradle.startParameter.isOffline
        outputs.upToDateWhen { !(onlyIfModified || overwrite) }
        onlyIf(Spec { // in case offline mode is enabled don't try to download if
            // destination already exists
            if (isOffline) {
                for (f in outputFiles)
                    if (f.exists()) {
                        if (!quiet)
                            project.logger.info("Skipping existing file '${f.name}' in offline mode.")
                    } else
                        throw IllegalStateException("Unable to download file '${f.name}' in offline mode.")
                return@Spec false
            }
            true
        })
    }

    /**
     * @return a list of files created by this task (i.e. the destination files)
     */
    val outputFiles: List<File>
        @OutputFiles
        get() = action.outputFiles

    override var src: Any?
        @Input
        get() = action.src
        set(value) {
            action.src = value
        }

    override var dest: File
        @Internal
        // see #getOutputFiles()
        get() = action.dest
        set(value) {
            action.dest = value
        }

    override var quiet: Boolean
        @Console
        get() = action.quiet
        set(value) {
            action.quiet = value
        }

    override var overwrite: Boolean
        @Input
        get() = action.overwrite
        set(value) {
            action.overwrite = value
        }

    override var onlyIfModified: Boolean
        @Input
        get() = action.onlyIfModified
        set(value) {
            action.onlyIfModified = value
        }

    override var onlyIfNewer: Boolean
        @Input
        get() = action.onlyIfNewer
        set(value) {
            action.onlyIfNewer = value
        }

    override var compress: Boolean
        @Input
        get() = action.compress
        set(value) {
            action.compress = value
        }

    override var username: String?
        @Optional @Input
        get() = action.username
        set(value) {
            action.username = value
        }

    override var password: String?
        @Optional @Input
        get() = action.password
        set(value) {
            action.password = value
        }

    override var authScheme: String?
        @Optional @Input
        get() = action.authScheme
        set(value) {
            action.authScheme = value
        }

    override var headers: MutableMap<String?, String?>?
        @Optional @Input
        get() = action.headers
        set(value) {
            action.headers = value
        }

    override fun header(name: String?, value: String?) = action.header(name, value)
    override fun getHeader(name: String?): String? = action.getHeader(name)

    override var acceptAnyCertificate: Boolean
        @Input
        get() = action.acceptAnyCertificate
        set(value) {
            action.acceptAnyCertificate = value
        }

    override var connectTimeout: ms
        @Input
        get() = action.connectTimeout
        set(value) {
            action.connectTimeout = value
        }

    override var readTimeout: ms
        @Input
        get() = action.readTimeout
        set(value) {
            action.readTimeout = value
        }

    override var retries: Int
        @Input
        get() = action.retries
        set(value) {
            action.retries = value
        }

    override var downloadTaskDir: File?
        @Internal
        get() = action.downloadTaskDir
        set(value) {
            action.downloadTaskDir = value
        }

    override var tempAndMove: Boolean
        @Input
        get() = action.tempAndMove
        set(value) {
            action.tempAndMove = value
        }

    override var useETag: Any?
        @Optional @Input
        get() = action.useETag
        set(value) {
            action.useETag = value
        }

    override var cachedETagsFile: File?
        @Internal
        get() = action.cachedETagsFile
        set(value) {
            action.cachedETagsFile = value
        }
}
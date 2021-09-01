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

import de.undercouch.gradle.tasks.download.internal.ms
import java.io.File
import java.net.MalformedURLException

/**
 * An interface for classes that perform file downloads
 * @author Michel Kraemer
 */
interface DownloadSpec {

    /** The download source URL */
    var src: Any?
        /**
         * The download source URL
         * @param src the URL
         * @throws MalformedURLException if the download source is not a URL
         */
        @Throws(MalformedURLException::class)
        set


    /** The download destination. A file or directory where to store the retrieved file */
    var dest: File

    /** The quiet flag */
    var quiet: Boolean

    /** The overwrite flag */
    var overwrite: Boolean

    /** The onlyIfModified flag.
     *  true if the file should only be downloaded if it has been modified on the server since the last download */
    var onlyIfModified: Boolean

    /** The onlyIfNewer flag. This method is an alias for [.onlyIfModified].
     * true if the file should only be downloaded if it  has been modified on the server since the last download */
    var onlyIfNewer: Boolean

    /** Specifies if compression should be used during download */
    var compress: Boolean

    /** The username for `Basic` or `Digest` authentication */
    var username: String?

    /** The password for `Basic` or `Digest` authentication */
    var password: String?

    /** The authentication scheme to use. This method accepts either a `String` (valid values are `"Basic"` and `"Digest"`).
     *
     * If `username` and `password` are set this method will only accept `"Basic"` or `"Digest"` as valid values.
     * The default value will be `"Basic"` in this case.
     */
    var authScheme: String?

    /** The HTTP request headers to use when downloading */
    var headers: MutableMap<String?, String?>?

    /**
     * Sets an HTTP request header to use when downloading
     * @param name name of the HTTP header
     * @param value value of the HTTP header
     */
    fun header(name: String?, value: String?)

    /**
     * @param name name of the HTTP header
     * @return the value of the HTTP header
     */
    fun getHeader(name: String?): String?

    /** If HTTPS certificate verification errors should be ignored and any certificate (even an invalid one) should be
     *  accepted. By default, certificates are validated and errors are not being ignored.
     *  @param accept true if certificate errors should be ignored (default: false) */
    var acceptAnyCertificate: Boolean

    /** Specifies the maximum time to wait in milliseconds until a connection is established. A value of zero means
     *  infinite timeout. A negative value is interpreted as undefined. */
    var connectTimeout: ms

    /** Specifies the maximum time in milliseconds to wait for data from the server. A value of zero means infinite
     *  timeout. A negative value is interpreted as undefined. */
    var readTimeout: ms

    /** Specifies the maximum number of retry attempts if a request has failed. By default, requests are never retried
     *  and the task fails immediately if the first request does not succeed. */
    var retries: Int

    /** Specifies the directory where gradle-download-task stores information that should persist between builds */
    var downloadTaskDir: File?

    /** Specifies whether the file should be downloaded to a temporary location and, upon successful execution, moved to
     *  the final location. If the overwrite flag is set to false, this flag is useful to avoid partially downloaded
     *  files if Gradle is forcefully closed or the system crashes.
     *  Note that the plugin always deletes partial downloads on connection errors, regardless of the value of this flag.
     *  The default temporary location can be configured with the [.downloadTaskDir];
     *  (default: false) */
    var tempAndMove: Boolean
        get() = false
        set(value) = TODO()

    /** The `useETag` flag. Possible values are:
     *      `true`: check if the entity tag (ETag) of a downloaded file has changed and issue a warning if a weak ETag was encountered
     *      `false`: Do not use entity tags (ETags) at all
     *      `"all"`: Use all ETags but do not issue a warning for weak ones
     *      `"strongOnly"`: Use only strong ETags
     *
     *  Note that this flag is only effective if `onlyIfModified` is `true`. */
    var useETag: Any?

    /** Sets the location of the file that keeps entity tags (ETags) received from the server
     *  (default: ${downloadTaskDir}/etags.json) */
    var cachedETagsFile: File?
}
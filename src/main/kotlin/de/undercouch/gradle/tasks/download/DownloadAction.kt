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

import de.undercouch.gradle.tasks.download.internal.*
import de.undercouch.gradle.tasks.download.internal.Helper.getFileFromDirectory
import de.undercouch.gradle.tasks.download.internal.Helper.getFileFromRegularFile
import de.undercouch.gradle.tasks.download.internal.Helper.isDirectory
import de.undercouch.gradle.tasks.download.internal.Helper.isRegularFile
import de.undercouch.gradle.tasks.download.internal.Helper.tryGetProvider
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.lang.Closure
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.internal.impldep.org.apache.http.*
import org.gradle.internal.impldep.org.apache.http.auth.AuthScheme
import org.gradle.internal.impldep.org.apache.http.auth.AuthScope
import org.gradle.internal.impldep.org.apache.http.auth.Credentials
import org.gradle.internal.impldep.org.apache.http.auth.UsernamePasswordCredentials
import org.gradle.internal.impldep.org.apache.http.client.AuthCache
import org.gradle.internal.impldep.org.apache.http.client.ClientProtocolException
import org.gradle.internal.impldep.org.apache.http.client.CredentialsProvider
import org.gradle.internal.impldep.org.apache.http.client.config.CookieSpecs
import org.gradle.internal.impldep.org.apache.http.client.config.RequestConfig
import org.gradle.internal.impldep.org.apache.http.client.methods.CloseableHttpResponse
import org.gradle.internal.impldep.org.apache.http.client.methods.HttpGet
import org.gradle.internal.impldep.org.apache.http.client.protocol.HttpClientContext
import org.gradle.internal.impldep.org.apache.http.client.utils.DateUtils
import org.gradle.internal.impldep.org.apache.http.impl.auth.BasicScheme
import org.gradle.internal.impldep.org.apache.http.impl.auth.DigestScheme
import org.gradle.internal.impldep.org.apache.http.impl.client.BasicAuthCache
import org.gradle.internal.impldep.org.apache.http.impl.client.BasicCredentialsProvider
import org.gradle.internal.impldep.org.apache.http.impl.client.CloseableHttpClient
import org.gradle.util.GradleVersion
import java.io.*
import java.lang.reflect.Array
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.*

/**
 * Downloads a file and displays progress
 * @author Michel Kraemer
 */
class DownloadAction @JvmOverloads constructor(project: Project, task: Task? = null) : DownloadSpec {

    private val projectApi: ProjectApiHelper
    private val logger: Logger
    private var servicesOwner: Any? = null
    private val isOffline: Boolean
    private val sourceObjects: MutableList<Any?> = ArrayList(1)
    private var cachedSources: MutableList<URL>? = null
    private var destObject: Any? = null
    private var cachedDest: File? = null
    override var quiet = false
    override var overwrite = true
    override var onlyIfModified = false
    override var onlyIfNewer: Boolean
        get() = onlyIfModified
        set(value) {
            onlyIfModified = value
        }
    override var compress = true
    override var username: String? = null
    override var password: String? = null
    override var authScheme: String? = null
        set(value) =
            if (value.equals("Basic", ignoreCase = true) || value.equals("Digest", ignoreCase = true))
                field = value
            else
                throw IllegalArgumentException("Invalid authentication scheme: '$value'. Valid values are 'Basic' and 'Digest'.")
    override var headers: MutableMap<String?, String?>? = null
        set(value) {
            if (field == null)
                field = LinkedHashMap()
            else
                field!!.clear()
            if (value != null)
                field!! += value
        }
    override var acceptAnyCertificate = false
    override var connectTimeout = 30 * 1000
    override var readTimeout = 30 * 1000
    override var retries = 0
    override var downloadTaskDir: File? = null
        set(value) {
            field = getDestinationFromDirProperty(value)
            requireNotNull(downloadTaskDir) { "download-task directory must either be a File or a CharSequence" }
        }
    override var tempAndMove = false
    override var useETag: Any?
        get() = useETagEnum.value
        set(value) {
            useETagEnum = UseETag.fromValue(value)
        }
    private var useETagEnum = UseETag.FALSE
    override var cachedETagsFile: File? = null
        get() = field ?: File(downloadTaskDir, "etags.json")
        set(value) {
            var location = value
            if (location is Closure<*>)
            //lazily evaluate closure
                location = location.call() as File?
            location = location.tryGetProvider() as File?
            field = when {
                location is CharSequence -> projectApi.file(location.toString())
                location.isRegularFile -> location.getFileFromRegularFile()
                location is File -> location
                else -> throw IllegalArgumentException("Location for cached ETags must either be a File or a CharSequence")
            }
        }
    private var progressLogger: ProgressLoggerWrapper? = null
    private var size: String? = null
    private var processedBytes: Long = 0
    private var loggedKb: Long = 0
    private var upToDate = 0

    /**
     * Starts downloading
     * @throws IOException if the file could not be downloaded
     */
    @Throws(IOException::class)
    fun execute() {
        check(!(GradleVersion.current() < HARD_MIN_GRADLE_VERSION && !quiet)) { "gradle-download-task requires " + "Gradle 2.x or higher" }
        if (GradleVersion.current() < SOFT_MIN_GRADLE_VERSION && !quiet)
            logger.warn("Support for running gradle-download-task with Gradle 2.x, 3.x, and 4.x has been deprecated " +
                                "and will be removed in gradle-download-task 5.0.0")
        check(!(JavaVersion.current() < JavaVersion.VERSION_1_7 && !quiet)) { "gradle-download-task requires Java 7 or higher" }
        if (JavaVersion.current() < JavaVersion.VERSION_1_8 && !quiet)
            logger.warn("Support for running gradle-download-task using Java 7 has been deprecated and will be removed in gradle-download-task 5.0.0")
        require(sourceObjects.isNotEmpty()) { "Please provide a download source" }
        requireNotNull(destObject) { "Please provide a download destination" }
        val sources = sources
        val dest = dest
        if (dest == projectApi.buildDirectory)
        //make sure build dir exists
            dest.mkdirs()
        if (sources.size > 1 && !dest.isDirectory)
            if (!dest.exists())
            // create directory automatically
                dest.mkdirs()
            else
                throw IllegalArgumentException("If multiple sources are provided the destination has to be a directory.")
        val clientFactory = CachingHttpClientFactory()
        try {
            for (src in sources)
                execute(src, clientFactory)
        } finally {
            clientFactory.close()
        }
    }

    @Throws(IOException::class)
    private fun execute(src: URL, clientFactory: HttpClientFactory) {
        val destFile = makeDestFile(src)
        if (!overwrite && destFile.exists()) {
            if (!quiet)
                logger.info("Destination file already exists. Skipping '${destFile.name}'")
            ++upToDate
            return
        }

        // in case offline mode is enabled don't try to download if
        // destination already exists
        if (isOffline) {
            if (destFile.exists()) {
                if (!quiet)
                    logger.info("Skipping existing file '${destFile.name}' in offline mode.")
                return
            }
            throw IllegalStateException("Unable to download file '$src' in offline mode.")
        }
        val timestamp = if (onlyIfModified && destFile.exists()) destFile.lastModified() else 0

        //create progress logger
        if (!quiet)
            try {
                progressLogger = ProgressLoggerWrapper(logger, servicesOwner!!, src.toString())
            } catch (e: Exception) {
                //unable to get progress logger
                logger.error("Unable to get progress logger. Download progress will not be displayed.")
            }
        if ("file" == src.protocol)
            executeFileProtocol(src, timestamp, destFile)
        else
            executeHttpProtocol(src, clientFactory, timestamp, destFile)
    }

    @Throws(IOException::class)
    private fun executeFileProtocol(src: URL, timestamp: Long, destFile: File) {
        var srcFile: File? = null
        try {
            srcFile = File(src.toURI())
            size = toLengthText(srcFile.length())
        } catch (e: URISyntaxException) {
            logger.warn("Unable to determine file length.")
        }

        //check if file was modified
        var lastModified: Long = 0
        if (srcFile != null) {
            lastModified = srcFile.lastModified()
            if (lastModified != 0L && timestamp >= lastModified) {
                if (!quiet)
                    logger.info("Not modified. Skipping '$src'")
                ++upToDate
                return
            }
        }
        val fileStream = BufferedInputStream(src.openStream())
        streamAndMove(fileStream, destFile)

        //set last-modified time of destination file
        if (onlyIfModified && lastModified > 0)
            destFile.setLastModified(lastModified)
    }

    @Throws(IOException::class)
    private fun executeHttpProtocol(src: URL, clientFactory: HttpClientFactory, timestamp: Long, destFile: File) {
        //create HTTP host from URL
        val httpHost = HttpHost(src.host, src.port, src.protocol)

        //create HTTP client
        val client: CloseableHttpClient = clientFactory.createHttpClient(httpHost, acceptAnyCertificate, retries)

        //open URL connection
        var etag: String? = null
        if (onlyIfModified && useETagEnum.enabled && destFile.exists()) {
            etag = getCachedETag(httpHost, src.file)
            if (!useETagEnum.useWeakETags && isWeakETag(etag))
                etag = null
        }
        val response: CloseableHttpResponse = openConnection(httpHost, src.file, timestamp, etag, client)
        response.use { resp ->
            //check if file on server was modified
            val lastModified = parseLastModified(resp)
            val code: Int = resp.statusLine.statusCode
            if (code == HttpStatus.SC_NOT_MODIFIED || lastModified != 0L && timestamp >= lastModified) {
                if (!quiet)
                    logger.info("Not modified. Skipping '$src'")
                ++upToDate
                return
            }

            //perform the download
            performDownload(resp, destFile)
        }

        //set last-modified time of destination file
        val newTimestamp = parseLastModified(response)
        if (onlyIfModified && newTimestamp > 0)
            destFile.setLastModified(newTimestamp)

        //store ETag
        if (onlyIfModified && useETagEnum.enabled)
            storeETag(httpHost, src.file, response)
    }

    /**
     * Save an HTTP response to a file
     * @param response the response to save
     * @param destFile the destination file
     * @throws IOException if the response could not be downloaded
     */
    @Throws(IOException::class)
    private fun performDownload(response: HttpResponse, destFile: File) {
        val entity: HttpEntity = response.entity ?: return

        //get content length
        val contentLength: Long = entity.contentLength
        if (contentLength >= 0)
            size = toLengthText(contentLength)
        processedBytes = 0
        loggedKb = 0

        //open stream and start downloading
        val input: InputStream = entity.content
        streamAndMove(input, destFile)
    }

    /**
     * Move a file by calling [File.renameTo] first and, if this
     * fails, by copying and deleting it.
     * @param src the file to move
     * @param dest the destination
     * @throws IOException if the file could not be moved
     */
    @Throws(IOException::class)
    private fun moveFile(src: File, dest: File) {
        if (src.renameTo(dest))
            return
        FileInputStream(src).use { stream(it, dest) }
        if (!src.delete())
            throw IOException("Could not delete temporary file '${src.absolutePath}' after copying it to '${dest.absolutePath}'.")
    }

    /**
     * If [.tempAndMove] is `true`, copy bytes from an input
     * stream to a temporary file and log progress. Upon successful
     * completion, move the temporary file to the given destination. If
     * [.tempAndMove] is `false`, just forward to
     * [.stream].
     * @param `is` the input stream to read
     * @param destFile the destination file
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun streamAndMove(input: InputStream, destFile: File) = when {
        !tempAndMove -> stream(input, destFile)
        else -> {
            //create parent directory
            downloadTaskDir!!.mkdirs()

            //create name of temporary file
            val tempFile = File.createTempFile(destFile.name, ".part", downloadTaskDir)

            //stream and move
            stream(input, tempFile)
            if (destFile.exists())
            //Delete destFile if it exists before renaming tempFile.
            //Otherwise, renaming might fail.
                if (!destFile.delete())
                    throw IOException("Could not delete old destination file '${destFile.absolutePath}'.")
            try {
                moveFile(tempFile, destFile)
            } catch (e: IOException) {
                throw IOException("Failed to move temporary file '${tempFile.absolutePath}' to destination file '${destFile.absolutePath}'.", e)
            }
        }
    }

    /**
     * Copy bytes from an input stream to a file and log progress
     * @param `is` the input stream to read
     * @param destFile the file to write to
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun stream(input: InputStream, destFile: File) =
        try {
            startProgress()
            var finished = false
            try {
                FileOutputStream(destFile).use { os ->
                    val buf = ByteArray(1024 * 10)
                    var read: Int
                    while (input.read(buf).also { read = it } >= 0) {
                        os.write(buf, 0, read)
                        processedBytes += read.toLong()
                        logProgress()
                    }
                    os.flush()
                    finished = true
                }
            } finally {
                if (!finished)
                    destFile.delete()
            }
        } finally {
            input.close()
            completeProgress()
        }

    /**
     * Reads the [.cachedETagsFile]
     * @return a map containing the parsed contents of the cached etags file
     * or an empty map if the file does not exist yet
     */
    private fun readCachedETags(): MutableMap<String?, Any?> = when {
        cachedETagsFile!!.exists() -> JsonSlurper().parse(cachedETagsFile!!, "UTF-8") as MutableMap<String?, Any?>
        else -> LinkedHashMap()
    }

    /**
     * Get the cached ETag for the given host and file
     * @param host the host
     * @param file the file
     * @return the cached ETag or null if there is no ETag in the cache
     */
    private fun getCachedETag(host: HttpHost, file: String): String? {
        val cachedETags: Map<String?, Any?> = readCachedETags()
        val hostMap = cachedETags[host.toURI()] as Map<String, Any>? ?: return null
        val etagMap = hostMap[file] as Map<String, String>? ?: return null
        return etagMap["ETag"]
    }

    /**
     * Store the ETag header from the given response in the [.cachedETagsFile]
     * @param host the queried host
     * @param file the queried file
     * @param response the HTTP response
     * @throws IOException if the tag could not be written
     */
    @Throws(IOException::class)
    private fun storeETag(host: HttpHost, file: String, response: HttpResponse) {
        //get ETag header
        val etagHdr: Header = response.getFirstHeader("ETag")
        if (etagHdr == null) {
            if (!quiet)
                logger.warn("Server response does not include an entity tag (ETag).")
            return
        }
        val etag: String = etagHdr.value

        //handle weak ETags
        if (isWeakETag(etag)) {
            if (useETagEnum.displayWarningForWeak && !quiet)
                logger.warn("Weak entity tag (ETag) encountered. Please make sure you want to compare resources " +
                                    "based on weak ETags. If yes, set the 'useETag' flag to \"all\", otherwise set it to \"strongOnly\".")
            if (!useETagEnum.useWeakETags)
            //do not save weak etags
                return
        }

        //create directory for cached etags file
        cachedETagsFile?.parentFile?.mkdirs()

        //read existing cached etags file
        val cachedETags = readCachedETags()

        //create new entry in cached ETags file
        val etagMap: MutableMap<String, String> = LinkedHashMap()
        etagMap["ETag"] = etag
        val uri: String = host.toURI()
        val hostMap = cachedETags.getOrPut(uri) { LinkedHashMap<String?, Any?>() } as MutableMap<String?, Any?>
        hostMap[file] = etagMap

        //write cached ETags file
        val cachedETagsContents = JsonOutput.toJson(cachedETags)
        PrintWriter(cachedETagsFile!!, "UTF-8").use { writer ->
            writer.write(cachedETagsContents)
            writer.flush()
        }
    }

    /**
     * Checks if the given ETag is a weak one
     * @param etag the ETag
     * @return true if `etag` is weak
     */
    private fun isWeakETag(etag: String?): Boolean = etag != null && etag.startsWith("W/")

    /**
     * Generates the path to an output file for a given source URL. Creates
     * all necessary parent directories for the destination file.
     * @param src the source
     * @return the path to the output file
     */
    private fun makeDestFile(src: URL): File {
        var destFile = dest ?: throw IllegalArgumentException("Please provide a download destination")
        if (destFile.isDirectory) {
            //guess name from URL
            var name = src.toString()
            if (name.endsWith("/"))
                name = name.substring(0, name.length - 1)
            name = name.substring(name.lastIndexOf('/') + 1)
            destFile = File(destFile, name)
        } else
        //create destination directory
            destFile.parentFile?.mkdirs()
        return destFile
    }

    /**
     * Opens a connection to the given HTTP host and requests a file. Checks
     * the last-modified header on the server if the given timestamp is
     * greater than 0.
     * @param httpHost the HTTP host to connect to
     * @param file the file to request
     * @param timestamp the timestamp of the destination file, in milliseconds
     * @param etag the cached ETag for the requested host and file
     * @param client the HTTP client to use to perform the request
     * @return the URLConnection
     * @throws IOException if the connection could not be opened
     */
    @Throws(IOException::class)
    private fun openConnection(httpHost: HttpHost, file: String, timestamp: Long, etag: String?,
                               client: CloseableHttpClient): CloseableHttpResponse {
        //perform preemptive authentication
        var context: HttpClientContext? = null
        if (username != null && password != null) {
            context = HttpClientContext.create()
            val scheme: AuthScheme = when {
                "Digest".equals(authScheme, ignoreCase = true) -> DigestScheme()
                else -> BasicScheme()
            }
            val c: Credentials = UsernamePasswordCredentials(username, password)
            addAuthentication(httpHost, c, scheme, context)
        }

        //create request
        val get = HttpGet(file)

        //configure timeouts
        val config: RequestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setConnectionRequestTimeout(connectTimeout)
            .setSocketTimeout(readTimeout)
            .setCookieSpec(CookieSpecs.STANDARD)
            .setContentCompressionEnabled(compress)
            .build()
        get.config = config

        //add authentication information for proxy
        val scheme: String = httpHost.schemeName
        val proxyHost = System.getProperty("$scheme.proxyHost")
        val proxyPort = System.getProperty("$scheme.proxyPort")
        val proxyUser = System.getProperty("$scheme.proxyUser")
        val proxyPassword = System.getProperty("$scheme.proxyPassword")
        if (proxyHost != null && proxyPort != null && proxyUser != null && proxyPassword != null) {
            if (context == null)
                context = HttpClientContext.create()
            val nProxyPort = proxyPort.toInt()
            val proxy = HttpHost(proxyHost, nProxyPort, scheme)
            val credentials: Credentials = UsernamePasswordCredentials(proxyUser, proxyPassword)
            addAuthentication(proxy, credentials, null, context!!)
        }

        //set If-Modified-Since header
        if (timestamp > 0)
            get.setHeader("If-Modified-Since", DateUtils.formatDate(Date(timestamp)))

        //set If-None-Match header
        if (etag != null)
            get.setHeader("If-None-Match", etag)

        //set headers
        if (headers != null)
            for ((key, value) in headers!!)
                get.addHeader(key, value)

        //execute request
        val response: CloseableHttpResponse = client.execute(httpHost, get, context)

        //handle response
        val code: Int = response.statusLine.statusCode
        if ((code < 200 || code > 299) && code != HttpStatus.SC_NOT_MODIFIED) {
            var phrase: String = response.statusLine.reasonPhrase
            val url: String = httpHost.toString() + file
            if (phrase == null || phrase.isEmpty())
                phrase = "HTTP status code: $code, URL: $url"
            else
                phrase += " (HTTP status code: $code, URL: $url)"
            response.close()
            throw ClientProtocolException(phrase)
        }
        return response
    }

    /**
     * Add authentication information for the given host
     * @param host the host
     * @param credentials the credentials
     * @param authScheme the scheme for preemptive authentication (should be
     * `null` if adding authentication for a proxy server)
     * @param context the context in which the authentication information
     * should be saved
     */
    private fun addAuthentication(host: HttpHost, credentials: Credentials,
                                  authScheme: AuthScheme?, context: HttpClientContext) {
        var authCache: AuthCache? = context.authCache
        if (authCache == null) {
            authCache = BasicAuthCache()
            context.authCache = authCache
        }
        var credsProvider: CredentialsProvider? = context.credentialsProvider
        if (credsProvider == null) {
            credsProvider = BasicCredentialsProvider()
            context.credentialsProvider = credsProvider
        }
        credsProvider.setCredentials(AuthScope(host), credentials)
        if (authScheme != null)
            authCache.put(host, authScheme)
    }

    /**
     * Converts a number of bytes to a human-readable string
     * @param bytes the bytes
     * @return the human-readable string
     */
    private fun toLengthText(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> (bytes / 1024).toString() + " KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    /**
     * Parse the Last-Modified header of a [HttpResponse]
     * @param response the [HttpResponse]
     * @return the last-modified value or 0 if it is unknown
     */
    private fun parseLastModified(response: HttpResponse): Long {
        val header: Header = response.getLastHeader("Last-Modified") ?: return 0
        val value: String = header.value
        return when {
            value == null || value.isEmpty() -> 0
            else -> DateUtils.parseDate(value)?.time ?: 0
        }
    }

    private fun startProgress() {
        progressLogger?.started()
    }

    private fun completeProgress() {
        progressLogger?.completed()
    }

    private fun logProgress() {
        if (progressLogger == null)
            return
        val processedKb = processedBytes / 1024
        if (processedKb > loggedKb) {
            var msg = toLengthText(processedBytes)
            if (size != null)
                msg += "/$size"
            msg += " downloaded"
            progressLogger!!.progress(msg)
            loggedKb = processedKb
        }
    }

    /**
     * @return true if the download destination is up-to-date
     */
    fun isUpToDate(): Boolean = upToDate == sources.size

    /**
     * @return a list of files created by this action (i.e. the destination files)
     */
    val outputFiles: List<File>
        get() = sources.map(::makeDestFile)


    override var src: Any?
        get() = if (sources.size == 1) sources[0] else sources
        set(value) {
            sourceObjects.add(value)
        }

    override var dest: File
        get() {
            if (cachedDest != null)
                return cachedDest as File
            cachedDest = getDestinationFromDirProperty(destObject)
            requireNotNull(cachedDest) { "Download destination must be one of a File, Directory, RegularFile, or a CharSequence" }
            return cachedDest as File
        }
        set(value) {
            destObject = value
        }

    override fun header(name: String?, value: String?) {
        if (headers == null)
            headers = LinkedHashMap()
        headers!![name] = value
    }

    /**
     * Get a destination file from a property. This method accepts various
     * input objects and tries to convert them to a [File] object
     * @param dir the property
     * @return the [File] object or `null` if the property was
     * `null` or could not be converted
     */
    private fun getDestinationFromDirProperty(dir: Any?): File? {
        var dir = dir
        if (dir is Closure<*>)
        // lazily evaluate closure
            dir = dir.call()

        // lazily evaluate Provider
        return when (val d = dir.tryGetProvider()) {
            d is CharSequence -> projectApi.file(d.toString())
            d.isDirectory -> d.getFileFromDirectory().apply {
                // Make sure the directory exists, so we actually download to a file inside this directory.
                // Otherwise, we will just create a file with the name of this directory.
                mkdirs()
            }
            d.isRegularFile -> d.getFileFromRegularFile()
            else -> d as? File?
        }
    }

    /**
     * Recursively convert the given source to a list of URLs
     * @param src the source to convert
     * @return the list of URLs
     */
    private fun convertSource(src: Any?): List<URL> {
        var src = src
        val result: MutableList<URL> = ArrayList()
        if (src is Closure<*>) {
            // lazily evaluate closure
            src = src.call()
        }
        src = src.tryGetProvider()
        if (src is CharSequence) {
            try {
                result.add(URL(src.toString()))
            } catch (e: MalformedURLException) {
                throw IllegalArgumentException("Invalid source URL", e)
            }
        } else if (src is URL) {
            result.add(src)
        } else if (src is Collection<*>) {
            for (sco in src) {
                result += convertSource(sco)
            }
        } else if (src != null && src.javaClass.isArray) {
            val len = Array.getLength(src)
            for (i in 0 until len) {
                val sco = Array.get(src, i)
                result += convertSource(sco)
            }
        } else {
            throw IllegalArgumentException("Download source must " +
                                                   "either be a URL, a CharSequence, a Collection or an array.")
        }
        return result
    }

    /**
     * Evaluate [.sourceObjects] and return a list of source URLs.
     * Cache the result in [.cachedSources]
     * @return the list of URLs
     */
    private val sources: List<URL>
        get() {
            if (cachedSources != null)
                return cachedSources as List<URL>
            cachedSources = ArrayList(sourceObjects.size)
            for (src in sourceObjects) {
                cachedSources!! += convertSource(src)
            }
            return cachedSources!!
        }

    override fun getHeader(name: String?): String? = headers?.get(name)

    /**
     * Possible values for the "useETag" flag
     */
    enum class UseETag(val value: Any, val enabled: Boolean, val useWeakETags: Boolean,
                       val displayWarningForWeak: Boolean) {
        /**
         * Do not use ETags
         */
        FALSE(java.lang.Boolean.FALSE, false, false, false),

        /**
         * Use all ETags but display a warning for weak ones
         */
        TRUE(java.lang.Boolean.TRUE, true, true, true),

        /**
         * Use all ETags but do not display a warning for weak ones
         */
        ALL("all", true, true, false),

        /**
         * Use only strong ETags
         */
        STRONG_ONLY("strongOnly", true, false, false);

        companion object {
            fun fromValue(value: Any?): UseETag = when {
                TRUE.value == value -> TRUE
                FALSE.value == value -> FALSE
                else -> (value as? String)?.let {
                    when {
                        ALL.value == it -> ALL
                        STRONG_ONLY.value == it -> STRONG_ONLY
                        "true".equals(it, ignoreCase = true) -> TRUE
                        "false".equals(it, ignoreCase = true) -> TRUE
                        else -> null
                    }
                } ?: throw IllegalArgumentException("Illegal value for 'useETag' flag")
            }
        }
    }

    companion object {
        private val HARD_MIN_GRADLE_VERSION = GradleVersion.version("2.0")
        private val SOFT_MIN_GRADLE_VERSION = GradleVersion.version("5.0")
    }
    /**
     * Creates a new download action
     * @param project the project to be built
     * @param task the task to be executed, if applicable
     */
    /**
     * Creates a new download action
     * @param project the project to be built
     */
    init {
        // get required project properties now to enable configuration cache
        projectApi = ProjectApiHelper.newInstance(project)
        logger = project.logger
        servicesOwner = task ?: project
        isOffline = project.gradle.startParameter.isOffline
        downloadTaskDir = File(project.buildDir, "download-task")
    }
}
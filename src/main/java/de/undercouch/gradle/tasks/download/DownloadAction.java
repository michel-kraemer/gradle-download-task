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

package de.undercouch.gradle.tasks.download;

import de.undercouch.gradle.tasks.download.internal.CachingHttpClientFactory;
import de.undercouch.gradle.tasks.download.internal.HttpClientFactory;
import de.undercouch.gradle.tasks.download.internal.ProgressLoggerWrapper;
import de.undercouch.gradle.tasks.download.internal.ProjectApiHelper;
import de.undercouch.gradle.tasks.download.org.apache.http.Header;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpEntity;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpHost;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpResponse;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpStatus;
import de.undercouch.gradle.tasks.download.org.apache.http.auth.AuthScheme;
import de.undercouch.gradle.tasks.download.org.apache.http.auth.AuthScope;
import de.undercouch.gradle.tasks.download.org.apache.http.auth.Credentials;
import de.undercouch.gradle.tasks.download.org.apache.http.auth.UsernamePasswordCredentials;
import de.undercouch.gradle.tasks.download.org.apache.http.client.AuthCache;
import de.undercouch.gradle.tasks.download.org.apache.http.client.ClientProtocolException;
import de.undercouch.gradle.tasks.download.org.apache.http.client.CredentialsProvider;
import de.undercouch.gradle.tasks.download.org.apache.http.client.config.CookieSpecs;
import de.undercouch.gradle.tasks.download.org.apache.http.client.config.RequestConfig;
import de.undercouch.gradle.tasks.download.org.apache.http.client.methods.CloseableHttpResponse;
import de.undercouch.gradle.tasks.download.org.apache.http.client.methods.HttpGet;
import de.undercouch.gradle.tasks.download.org.apache.http.client.protocol.HttpClientContext;
import de.undercouch.gradle.tasks.download.org.apache.http.client.utils.DateUtils;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.auth.BasicScheme;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.auth.DigestScheme;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.client.BasicAuthCache;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.client.BasicCredentialsProvider;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.client.CloseableHttpClient;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.util.GradleVersion;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.undercouch.gradle.tasks.download.internal.DirectoryHelper.getFileFromDirectory;
import static de.undercouch.gradle.tasks.download.internal.DirectoryHelper.isDirectory;
import static de.undercouch.gradle.tasks.download.internal.ProviderHelper.tryGetProvider;
import static de.undercouch.gradle.tasks.download.internal.RegularFileHelper.getFileFromRegularFile;
import static de.undercouch.gradle.tasks.download.internal.RegularFileHelper.isRegularFile;

/**
 * Downloads a file and displays progress
 * @author Michel Kraemer
 */
public class DownloadAction implements DownloadSpec {
    private static final GradleVersion HARD_MIN_GRADLE_VERSION =
            GradleVersion.version("2.0");
    private static final GradleVersion SOFT_MIN_GRADLE_VERSION =
            GradleVersion.version("5.0");

    private final ProjectApiHelper projectApi;
    private final Logger logger;
    private final Object servicesOwner;
    private final boolean isOffline;
    private final List<Object> sourceObjects = new ArrayList<>(1);
    private List<URL> cachedSources;
    private Object destObject;
    private File cachedDest;
    private boolean quiet = false;
    private boolean overwrite = true;
    private boolean onlyIfModified = false;
    private boolean compress = true;
    private String username;
    private String password;
    private String authScheme;
    private Map<String, String> headers;
    private boolean acceptAnyCertificate = false;
    private int connectTimeoutMs = 30 * 1000;
    private int readTimeoutMs = 30 * 1000;
    private int retries = 0;
    private File downloadTaskDir;
    private boolean tempAndMove = false;
    private UseETag useETag = UseETag.FALSE;
    private File cachedETagsFile;

    private ProgressLoggerWrapper progressLogger;
    private String size;
    private long processedBytes = 0;
    private long loggedKb = 0;

    private int upToDate = 0;

    /**
     * Creates a new download action
     * @param project the project to be built
     */
    public DownloadAction(Project project) {
        this(project, null);
    }

    /**
     * Creates a new download action
     * @param project the project to be built
     * @param task the task to be executed, if applicable
     */
    public DownloadAction(Project project, @Nullable Task task) {
        // get required project properties now to enable configuration cache
        this.projectApi = ProjectApiHelper.newInstance(project);
        this.logger = project.getLogger();
        if (task != null) {
            this.servicesOwner = task;
        } else {
            this.servicesOwner = project;
        }
        this.isOffline = project.getGradle().getStartParameter().isOffline();
        this.downloadTaskDir = new File(project.getBuildDir(), "download-task");
    }

    /**
     * Starts downloading
     * @throws IOException if the file could not downloaded
     */
    public void execute() throws IOException {
        if (GradleVersion.current().compareTo(HARD_MIN_GRADLE_VERSION) < 0 && !quiet) {
            throw new IllegalStateException("gradle-download-task requires " +
                    "Gradle 2.x or higher");
        }
        if (GradleVersion.current().compareTo(SOFT_MIN_GRADLE_VERSION) < 0 && !quiet) {
            logger.warn("Support for running gradle-download-task "
                    + "with Gradle 2.x, 3.x, and 4.x has been deprecated and will be removed in "
                    + "gradle-download-task 5.0.0");
        }
        if (JavaVersion.current().compareTo(JavaVersion.VERSION_1_7) < 0 && !quiet) {
            throw new IllegalStateException("gradle-download-task requires " +
                    "Java 7 or higher");
        }
        if (JavaVersion.current().compareTo(JavaVersion.VERSION_1_8) < 0 && !quiet) {
            logger.warn("Support for running gradle-download-task "
                    + "using Java 7 has been deprecated and will be removed in "
                    + "gradle-download-task 5.0.0");
        }

        if (sourceObjects.isEmpty()) {
            throw new IllegalArgumentException("Please provide a download source");
        }
        if (destObject == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }

        List<URL> sources = getSources();
        File dest = getDest();

        if (dest.equals(projectApi.getBuildDirectory())) {
            //make sure build dir exists
            dest.mkdirs();
        }
        
        if (sources.size() > 1 && !dest.isDirectory()) {
            if (!dest.exists()) {
                // create directory automatically
                dest.mkdirs();
            } else {
                throw new IllegalArgumentException("If multiple sources are provided "
                        + "the destination has to be a directory.");
            }
        }
        
        CachingHttpClientFactory clientFactory = new CachingHttpClientFactory();
        try {
            for (URL src : sources) {
                execute(src, clientFactory);
            }
        } finally {
            clientFactory.close();
        }
    }

    private void execute(URL src, HttpClientFactory clientFactory) throws IOException {
        final File destFile = makeDestFile(src);
        if (!overwrite && destFile.exists()) {
            if (!quiet) {
                logger.info("Destination file already exists. "
                        + "Skipping '" + destFile.getName() + "'");
            }
            ++upToDate;
            return;
        }
        
        // in case offline mode is enabled don't try to download if
        // destination already exists
        if (isOffline) {
            if (destFile.exists()) {
                if (!quiet) {
                    logger.info("Skipping existing file '" +
                            destFile.getName() + "' in offline mode.");
                }
                return;
            }
            throw new IllegalStateException("Unable to download file '" + src +
                    "' in offline mode.");
        }

        final long timestamp = onlyIfModified && destFile.exists() ? destFile.lastModified() : 0;
        
        //create progress logger
        if (!quiet) {
            try {
                progressLogger = new ProgressLoggerWrapper(logger, servicesOwner, src.toString());
            } catch (Exception e) {
                //unable to get progress logger
                logger.error("Unable to get progress logger. Download "
                        + "progress will not be displayed.");
            }
        }

        if ("file".equals(src.getProtocol())) {
            executeFileProtocol(src, timestamp, destFile);
        } else {
            executeHttpProtocol(src, clientFactory, timestamp, destFile);
        }
    }

    private void executeFileProtocol(URL src, long timestamp, File destFile)
            throws IOException {
        File srcFile = null;
        try {
            srcFile = new File(src.toURI());
            size = toLengthText(srcFile.length());
        } catch (URISyntaxException e) {
            logger.warn("Unable to determine file length.");
        }
        
        //check if file was modified
        long lastModified = 0;
        if (srcFile != null) {
            lastModified = srcFile.lastModified();
            if (lastModified != 0 && timestamp >= lastModified) {
                if (!quiet) {
                    logger.info("Not modified. Skipping '" + src + "'");
                }
                ++upToDate;
                return;
            }
        }

        BufferedInputStream fileStream = new BufferedInputStream(src.openStream());
        streamAndMove(fileStream, destFile);
        
        //set last-modified time of destination file
        if (onlyIfModified && lastModified > 0) {
            destFile.setLastModified(lastModified);
        }
    }

    private void executeHttpProtocol(URL src, HttpClientFactory clientFactory,
            long timestamp, File destFile) throws IOException {
        //create HTTP host from URL
        HttpHost httpHost = new HttpHost(src.getHost(), src.getPort(), src.getProtocol());
        
        //create HTTP client
        CloseableHttpClient client = clientFactory.createHttpClient(
                httpHost, acceptAnyCertificate, retries);

        //open URL connection
        String etag = null;
        if (onlyIfModified && useETag.enabled && destFile.exists()) {
            etag = getCachedETag(httpHost, src.getFile());
            if (!useETag.useWeakETags && isWeakETag(etag)) {
                etag = null;
            }
        }
        CloseableHttpResponse response = openConnection(httpHost, src.getFile(),
                timestamp, etag, client);
        try {
            //check if file on server was modified
            long lastModified = parseLastModified(response);
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_NOT_MODIFIED ||
                    (lastModified != 0 && timestamp >= lastModified)) {
                if (!quiet) {
                    logger.info("Not modified. Skipping '" + src + "'");
                }
                ++upToDate;
                return;
            }
        
            //perform the download
            performDownload(response, destFile);
        } finally {
            response.close();
        }
        
        //set last-modified time of destination file
        long newTimestamp = parseLastModified(response);
        if (onlyIfModified && newTimestamp > 0) {
            destFile.setLastModified(newTimestamp);
        }

        //store ETag
        if (onlyIfModified && useETag.enabled) {
            storeETag(httpHost, src.getFile(), response);
        }
    }

    /**
     * Save an HTTP response to a file
     * @param response the response to save
     * @param destFile the destination file
     * @throws IOException if the response could not be downloaded
     */
    private void performDownload(HttpResponse response, File destFile)
            throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return;
        }
        
        //get content length
        long contentLength = entity.getContentLength();
        if (contentLength >= 0) {
            size = toLengthText(contentLength);

        }
        processedBytes = 0;
        loggedKb = 0;
        
        //open stream and start downloading
        InputStream is = entity.getContent();
        streamAndMove(is, destFile);
    }

    /**
     * Move a file by calling {@link File#renameTo(File)} first and, if this
     * fails, by copying and deleting it.
     * @param src the file to move
     * @param dest the destination
     * @throws IOException if the file could not be moved
     */
    private void moveFile(File src, File dest) throws IOException {
        if (src.renameTo(dest)) {
            return;
        }

        // renameTo() failed. Try to copy the file and delete it afterwards.
        // see issue #146
        try (InputStream is = new FileInputStream(src)) {
            stream(is, dest);
        }
        if (!src.delete()) {
            throw new IOException("Could not delete temporary file '" +
                    src.getAbsolutePath() + "' after copying it to '" +
                    dest.getAbsolutePath() + "'.");
        }
    }

    /**
     * If {@link #tempAndMove} is <code>true</code>, copy bytes from an input
     * stream to a temporary file and log progress. Upon successful
     * completion, move the temporary file to the given destination. If
     * {@link #tempAndMove} is <code>false</code>, just forward to
     * {@link #stream(InputStream, File)}.
     * @param is the input stream to read
     * @param destFile the destination file
     * @throws IOException if an I/O error occurs
     */
    private void streamAndMove(InputStream is, File destFile) throws IOException {
        if (!tempAndMove) {
            stream(is, destFile);
        } else {
            //create parent directory
            downloadTaskDir.mkdirs();

            //create name of temporary file
            File tempFile = File.createTempFile(destFile.getName(), ".part",
                    this.downloadTaskDir);

            //stream and move
            stream(is, tempFile);
            if (destFile.exists()) {
                //Delete destFile if it exists before renaming tempFile.
                //Otherwise renaming might fail.
                if (!destFile.delete()) {
                    throw new IOException("Could not delete old destination file '" +
                            destFile.getAbsolutePath() + "'.");
                }
            }
            try {
                moveFile(tempFile, destFile);
            } catch (IOException e) {
                throw new IOException("Failed to move temporary file '" +
                        tempFile.getAbsolutePath() + "' to destination file '" +
                        destFile.getAbsolutePath() + "'.", e);
            }
        }
    }

    /**
     * Copy bytes from an input stream to a file and log progress
     * @param is the input stream to read
     * @param destFile the file to write to
     * @throws IOException if an I/O error occurs
     */
    private void stream(InputStream is, File destFile) throws IOException {
        try {
            startProgress();

            boolean finished = false;
            try (OutputStream os = new FileOutputStream(destFile)) {
                byte[] buf = new byte[1024 * 10];
                int read;
                while ((read = is.read(buf)) >= 0) {
                    os.write(buf, 0, read);
                    processedBytes += read;
                    logProgress();
                }

                os.flush();
                finished = true;
            } finally {
                if (!finished) {
                    destFile.delete();
                }
            }
        } finally {
            is.close();
            completeProgress();
        }
    }

    /**
     * Reads the {@link #cachedETagsFile}
     * @return a map containing the parsed contents of the cached etags file
     * or an empty map if the file does not exist yet
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readCachedETags() {
        File cachedETagsFile = getCachedETagsFile();
        Map<String, Object> cachedETags;
        if (cachedETagsFile.exists()) {
            JsonSlurper slurper = new JsonSlurper();
            cachedETags = (Map<String, Object>)slurper.parse(cachedETagsFile, "UTF-8");
        } else {
            cachedETags = new LinkedHashMap<>();
        }
        return cachedETags;
    }

    /**
     * Get the cached ETag for the given host and file
     * @param host the host
     * @param file the file
     * @return the cached ETag or null if there is no ETag in the cache
     */
    private String getCachedETag(HttpHost host, String file) {
        Map<String, Object> cachedETags = readCachedETags();

        @SuppressWarnings("unchecked")
        Map<String, Object> hostMap =
                (Map<String, Object>)cachedETags.get(host.toURI());
        if (hostMap == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> etagMap = (Map<String, String>)hostMap.get(file);
        if (etagMap == null) {
            return null;
        }

        return etagMap.get("ETag");
    }

    /**
     * Store the ETag header from the given response in the {@link #cachedETagsFile}
     * @param host the queried host
     * @param file the queried file
     * @param response the HTTP response
     * @throws IOException if the tag could not be written
     */
    @SuppressWarnings("unchecked")
    private void storeETag(HttpHost host, String file, HttpResponse response)
            throws IOException {
        //get ETag header
        Header etagHdr = response.getFirstHeader("ETag");
        if (etagHdr == null) {
            if (!quiet) {
                logger.warn("Server response does not include an "
                        + "entity tag (ETag).");
            }
            return;
        }
        String etag = etagHdr.getValue();

        //handle weak ETags
        if (isWeakETag(etag)) {
            if (useETag.displayWarningForWeak && !quiet) {
                logger.warn("Weak entity tag (ETag) encountered. "
                        + "Please make sure you want to compare resources based on "
                        + "weak ETags. If yes, set the 'useETag' flag to \"all\", "
                        + "otherwise set it to \"strongOnly\".");
            }
            if (!useETag.useWeakETags) {
                //do not save weak etags
                return;
            }
        }

        //create directory for cached etags file
        File parent = getCachedETagsFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        //read existing cached etags file
        Map<String, Object> cachedETags = readCachedETags();

        //create new entry in cached ETags file
        Map<String, String> etagMap = new LinkedHashMap<>();
        etagMap.put("ETag", etag);

        String uri = host.toURI();
        Map<String, Object> hostMap = (Map<String, Object>)cachedETags.get(uri);
        if (hostMap == null) {
            hostMap = new LinkedHashMap<>();
            cachedETags.put(uri, hostMap);
        }
        hostMap.put(file, etagMap);

        //write cached ETags file
        String cachedETagsContents = JsonOutput.toJson(cachedETags);
        try (PrintWriter writer = new PrintWriter(getCachedETagsFile(), "UTF-8")) {
            writer.write(cachedETagsContents);
            writer.flush();
        }
    }

    /**
     * Checks if the given ETag is a weak one
     * @param etag the ETag
     * @return true if <code>etag</code> is weak
     */
    private boolean isWeakETag(String etag) {
        return etag != null && etag.startsWith("W/");
    }

    /**
     * Generates the path to an output file for a given source URL. Creates
     * all necessary parent directories for the destination file.
     * @param src the source
     * @return the path to the output file
     */
    private File makeDestFile(URL src) {
        File destFile = getDest();
        if (destFile == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }

        if (destFile.isDirectory()) {
            //guess name from URL
            String name = src.toString();
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            name = name.substring(name.lastIndexOf('/') + 1);
            destFile = new File(destFile, name);
        } else {
            //create destination directory
            File parent = destFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
        }
        return destFile;
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
    private CloseableHttpResponse openConnection(HttpHost httpHost, String file,
            long timestamp, String etag, CloseableHttpClient client) throws IOException {
        //perform preemptive authentication
        HttpClientContext context = null;
        if (username != null && password != null) {
            context = HttpClientContext.create();
            AuthScheme as;
            if ("Digest".equalsIgnoreCase(authScheme)) {
                as = new DigestScheme();
            } else {
                as = new BasicScheme();
            }
            Credentials c = new UsernamePasswordCredentials(username, password);
            addAuthentication(httpHost, c, as, context);
        }
        
        //create request
        HttpGet get = new HttpGet(file);

        //configure timeouts
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectTimeoutMs)
                .setSocketTimeout(readTimeoutMs)
                .setCookieSpec(CookieSpecs.STANDARD)
                .setContentCompressionEnabled(compress)
                .build();
        get.setConfig(config);

        //add authentication information for proxy
        String scheme = httpHost.getSchemeName();
        String proxyHost = System.getProperty(scheme + ".proxyHost");
        String proxyPort = System.getProperty(scheme + ".proxyPort");
        String proxyUser = System.getProperty(scheme + ".proxyUser");
        String proxyPassword = System.getProperty(scheme + ".proxyPassword");
        if (proxyHost != null && proxyPort != null &&
                proxyUser != null && proxyPassword != null) {
            if (context == null) {
                context = HttpClientContext.create();
            }
            int nProxyPort = Integer.parseInt(proxyPort);
            HttpHost proxy = new HttpHost(proxyHost, nProxyPort, scheme);
            Credentials credentials = new UsernamePasswordCredentials(
                    proxyUser, proxyPassword);
            addAuthentication(proxy, credentials, null, context);
        }
        
        //set If-Modified-Since header
        if (timestamp > 0) {
            get.setHeader("If-Modified-Since", DateUtils.formatDate(new Date(timestamp)));
        }
        
        //set If-None-Match header
        if (etag != null) {
            get.setHeader("If-None-Match", etag);
        }
        
        //set headers
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                get.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        
        //execute request
        CloseableHttpResponse response = client.execute(httpHost, get, context);
        
        //handle response
        int code = response.getStatusLine().getStatusCode();
        if ((code < 200 || code > 299) && code != HttpStatus.SC_NOT_MODIFIED) {
            String phrase = response.getStatusLine().getReasonPhrase();
            String url = httpHost + file;
            if (phrase == null || phrase.isEmpty()) {
                phrase = "HTTP status code: " + code + ", URL: " + url;
            } else {
                phrase += " (HTTP status code: " + code + ", URL: " + url + ")";
            }
            response.close();
            throw new ClientProtocolException(phrase);
        }
        
        return response;
    }
    
    /**
     * Add authentication information for the given host
     * @param host the host
     * @param credentials the credentials
     * @param authScheme the scheme for preemptive authentication (should be
     * <code>null</code> if adding authentication for a proxy server)
     * @param context the context in which the authentication information
     * should be saved
     */
    private void addAuthentication(HttpHost host, Credentials credentials,
            AuthScheme authScheme, HttpClientContext context) {
        AuthCache authCache = context.getAuthCache();
        if (authCache == null) {
            authCache = new BasicAuthCache();
            context.setAuthCache(authCache);
        }
        
        CredentialsProvider credsProvider = context.getCredentialsProvider();
        if (credsProvider == null) {
            credsProvider = new BasicCredentialsProvider();
            context.setCredentialsProvider(credsProvider);
        }
        
        credsProvider.setCredentials(new AuthScope(host), credentials);
        
        if (authScheme != null) {
            authCache.put(host, authScheme);
        }
    }
    
    /**
     * Converts a number of bytes to a human-readable string
     * @param bytes the bytes
     * @return the human-readable string
     */
    private String toLengthText(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Parse the Last-Modified header of a {@link HttpResponse}
     * @param response the {@link HttpResponse}
     * @return the last-modified value or 0 if it is unknown
     */
    private long parseLastModified(HttpResponse response) {
        Header header = response.getLastHeader("Last-Modified");
        if (header == null) {
            return 0;
        }
        String value = header.getValue();
        if (value == null || value.isEmpty()) {
            return 0;
        }
        Date date = DateUtils.parseDate(value);
        if (date == null) {
            return 0;
        }
        return date.getTime();
    }
    
    private void startProgress() {
        if (progressLogger != null) {
            progressLogger.started();
        }
    }
    
    private void completeProgress() {
        if (progressLogger != null) {
            progressLogger.completed();
        }
    }
    
    private void logProgress() {
        if (progressLogger == null) {
            return;
        }
        
        long processedKb = processedBytes / 1024;
        if (processedKb > loggedKb) {
            String msg = toLengthText(processedBytes);
            if (size != null) {
                msg += "/" + size;
            }
            msg += " downloaded";
            progressLogger.progress(msg);
            loggedKb = processedKb;
        }
    }
    
    /**
     * @return true if the download destination is up to date
     */
    public boolean isUpToDate() {
        return upToDate == getSources().size();
    }

    /**
     * @return a list of files created by this action (i.e. the destination files)
     */
    public List<File> getOutputFiles() {
        List<URL> sources = getSources();
        List<File> files = new ArrayList<>(sources.size());
        for (URL src : sources) {
            files.add(makeDestFile(src));
        }
        return files;
    }
    
    @Override
    public void src(Object src) {
        sourceObjects.add(src);
    }
    
    @Override
    public void dest(Object dest) {
        destObject = dest;
    }
    
    @Override
    public void quiet(boolean quiet) {
        this.quiet = quiet;
    }
    
    @Override
    public void overwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
    
    @Override
    public void onlyIfModified(boolean onlyIfModified) {
        this.onlyIfModified = onlyIfModified;
    }
    
    @Override
    public void onlyIfNewer(boolean onlyIfNewer) {
        onlyIfModified(onlyIfNewer);
    }
    
    @Override
    public void compress(boolean compress) {
        this.compress = compress;
    }
    
    @Override
    public void username(String username) {
        this.username = username;
    }
    
    @Override
    public void password(String password) {
        this.password = password;
    }

    @Override
    public void authScheme(String authScheme) {
        if (authScheme.equalsIgnoreCase("Basic") ||
                authScheme.equalsIgnoreCase("Digest")) {
            this.authScheme = authScheme;
        } else {
            throw new IllegalArgumentException("Invalid authentication scheme: "
                    + "'" + authScheme + "'. Valid values are 'Basic' and 'Digest'.");
        }
    }

    @Override
    public void headers(Map<String, String> headers) {
        if (this.headers == null) {
            this.headers = new LinkedHashMap<>();
        } else {
            this.headers.clear();
        }
        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    @Override
    public void header(String name, String value) {
        if (headers == null) {
            headers = new LinkedHashMap<>();
        }
        headers.put(name, value);
    }

    @Override
    public void acceptAnyCertificate(boolean accept) {
        this.acceptAnyCertificate = accept;
    }

    @Override
    public void connectTimeout(int milliseconds) {
        this.connectTimeoutMs = milliseconds;
    }

    @Override
    public void readTimeout(int milliseconds) {
        this.readTimeoutMs = milliseconds;
    }

    @Override
    public void retries(int retries) {
        this.retries = retries;
    }

    /**
     * Get a destination file from a property. This method accepts various
     * input objects and tries to convert them to a {@link File} object
     * @param dir the property
     * @return the {@link File} object or {@code null} if the property was
     * {@code null} or could not be converted
     */
    private File getDestinationFromDirProperty(Object dir) {
        if (dir instanceof Closure) {
            // lazily evaluate closure
            Closure<?> closure = (Closure<?>)dir;
            dir = closure.call();
        }

        // lazily evaluate Provider
        dir = tryGetProvider(dir);

        if (dir instanceof CharSequence) {
            return projectApi.file(dir.toString());
        } else if (isDirectory(dir)) {
            File f = getFileFromDirectory(dir);

            // Make sure the directory exists so we actually download to a file
            // inside this directory. Otherwise, we will just create a file
            // with the name of this directory.
            f.mkdirs();

            return f;
        } else if (isRegularFile(dir)) {
            return getFileFromRegularFile(dir);
        } else if (dir instanceof File) {
            return (File)dir;
        }

        return null;
    }

    @Override
    public void downloadTaskDir(Object dir) {
        if (dir instanceof Closure) {
            //lazily evaluate closure
            Closure<?> closure = (Closure<?>)dir;
            dir = closure.call();
        }

        dir = tryGetProvider(dir);

        if (dir instanceof CharSequence) {
            this.downloadTaskDir = projectApi.file(dir.toString());
        } else if (dir instanceof File) {
            this.downloadTaskDir = (File)dir;
        } else {
            throw new IllegalArgumentException("download-task directory must " +
                "either be a File or a CharSequence");
        }
    }

    @Override
    public void tempAndMove(boolean tempAndMove) {
        this.tempAndMove = tempAndMove;
    }

    @Override
    public void useETag(Object useETag) {
        this.useETag = UseETag.fromValue(useETag);
    }

    @Override
    public void cachedETagsFile(Object location) {
        if (location instanceof Closure) {
            //lazily evaluate closure
            Closure<?> closure = (Closure<?>)location;
            location = closure.call();
        }

        location = tryGetProvider(location);

        if (location instanceof CharSequence) {
            this.cachedETagsFile = projectApi.file(location.toString());
        } else if (location instanceof File) {
            this.cachedETagsFile = (File)location;
        } else {
            throw new IllegalArgumentException("Location for cached ETags must " +
                "either be a File or a CharSequence");
        }
    }

    /**
     * Recursively convert the given source to a list of URLs
     * @param src the source to convert
     * @return the list of URLs
     */
    private List<URL> convertSource(Object src) {
        List<URL> result = new ArrayList<>();

        if (src instanceof Closure) {
            // lazily evaluate closure
            Closure<?> closure = (Closure<?>)src;
            src = closure.call();
        }

        src = tryGetProvider(src);

        if (src instanceof CharSequence) {
            try {
                result.add(new URL(src.toString()));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid source URL", e);
            }
        } else if (src instanceof URL) {
            result.add((URL)src);
        } else if (src instanceof Collection) {
            Collection<?> sc = (Collection<?>)src;
            for (Object sco : sc) {
                result.addAll(convertSource(sco));
            }
        } else if (src != null && src.getClass().isArray()) {
            int len = Array.getLength(src);
            for (int i = 0; i < len; ++i) {
                Object sco = Array.get(src, i);
                result.addAll(convertSource(sco));
            }
        } else {
            throw new IllegalArgumentException("Download source must " +
                    "either be a URL, a CharSequence, a Collection or an array.");
        }

        return result;
    }

    /**
     * Evaluate {@link #sourceObjects} and return a list of source URLs.
     * Cache the result in {@link #cachedSources}
     * @return the list of URLs
     */
    private List<URL> getSources() {
        if (cachedSources != null) {
            return cachedSources;
        }

        cachedSources = new ArrayList<>(sourceObjects.size());
        for (Object src : sourceObjects) {
            cachedSources.addAll(convertSource(src));
        }

        return cachedSources;
    }

    @Override
    public Object getSrc() {
        List<URL> sources = getSources();
        if (sources.size() == 1) {
            return sources.get(0);
        }
        return sources;
    }

    @Override
    public File getDest() {
        if (cachedDest != null) {
            return cachedDest;
        }

        cachedDest = getDestinationFromDirProperty(destObject);
        if (cachedDest == null) {
            throw new IllegalArgumentException("Download destination must " +
                    "be one of a File, Directory, RegularFile, or a CharSequence");
        }

        return cachedDest;
    }
    
    @Override
    public boolean isQuiet() {
        return quiet;
    }
    
    @Override
    public boolean isOverwrite() {
        return overwrite;
    }
    
    @Override
    public boolean isOnlyIfModified() {
        return onlyIfModified;
    }
    
    @Override
    public boolean isOnlyIfNewer() {
        return isOnlyIfModified();
    }
    
    @Override
    public boolean isCompress() {
        return compress;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getAuthScheme() {
        return authScheme;
    }
    
    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getHeader(String name) {
        if (headers == null) {
            return null;
        }
        return headers.get(name);
    }

    @Override
    public boolean isAcceptAnyCertificate() {
        return acceptAnyCertificate;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeoutMs;
    }

    @Override
    public int getReadTimeout() {
        return readTimeoutMs;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    @Override
    public File getDownloadTaskDir() {
        return downloadTaskDir;
    }

    @Override
    public boolean isTempAndMove() {
        return tempAndMove;
    }

    @Override
    public Object getUseETag() {
        return useETag.value;
    }

    @Override
    public File getCachedETagsFile() {
        if (cachedETagsFile == null) {
            return new File(this.downloadTaskDir, "etags.json");
        }
        return cachedETagsFile;
    }

    /**
     * Possible values for the "useETag" flag
     */
    private enum UseETag {
        /**
         * Do not use ETags
         */
        FALSE(Boolean.FALSE, false, false, false),

        /**
         * Use all ETags but display a warning for weak ones
         */
        TRUE(Boolean.TRUE, true, true, true),

        /**
         * Use all ETags but do not display a warning for weak ones
         */
        ALL("all", true, true, false),

        /**
         * Use only strong ETags
         */
        STRONG_ONLY("strongOnly", true, false, false);

        final Object value;
        final boolean enabled;
        final boolean useWeakETags;
        final boolean displayWarningForWeak;

        UseETag(Object value, boolean useAnyETag, boolean useWeakETags,
                boolean displayWarningForWeak) {
            this.value = value;
            this.enabled = useAnyETag;
            this.useWeakETags = useWeakETags;
            this.displayWarningForWeak = displayWarningForWeak;
        }

        static UseETag fromValue(Object value) {
            if (TRUE.value.equals(value)) {
                return TRUE;
            } else if (FALSE.value.equals(value)) {
                return FALSE;
            } else if (value instanceof String) {
                String s = (String)value;
                if (ALL.value.equals(s)) {
                    return ALL;
                } else if (STRONG_ONLY.value.equals(s)) {
                    return STRONG_ONLY;
                } else if ("true".equalsIgnoreCase(s)) {
                    return TRUE;
                } else if ("false".equalsIgnoreCase(s)) {
                    return TRUE;
                }
            }
            throw new IllegalArgumentException("Illegal value for 'useETag' flag");
        }
    }
}

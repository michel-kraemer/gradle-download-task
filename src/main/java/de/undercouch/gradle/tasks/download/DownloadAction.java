package de.undercouch.gradle.tasks.download;

import de.undercouch.gradle.tasks.download.internal.CachingHttpClientFactory;
import de.undercouch.gradle.tasks.download.internal.HttpClientFactory;
import de.undercouch.gradle.tasks.download.internal.ProgressLoggerWrapper;
import de.undercouch.gradle.tasks.download.internal.WorkerExecutorFuture;
import de.undercouch.gradle.tasks.download.internal.WorkerExecutorHelper;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import kotlin.jvm.functions.Function0;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.util.GradleVersion;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Downloads a file and displays progress
 * @author Michel Kraemer
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "CommentedOutCode"})
public class DownloadAction implements DownloadSpec, Serializable {
    private static final GradleVersion HARD_MIN_GRADLE_VERSION =
            GradleVersion.version("5.0");
    // private static final GradleVersion SOFT_MIN_GRADLE_VERSION =
    //         GradleVersion.version("5.0");

    // HEADS UP: FIELDS ARE POTENTIALLY ACCESSED BY MULTIPLE THREADS!
    private final ProjectLayout projectLayout;
    private final Logger logger;
    private final Object servicesOwner;
    private final ObjectFactory objectFactory;
    private final boolean isOffline;
    private final List<Object> sourceObjects = new ArrayList<>(1);
    private List<URL> cachedSources;
    private transient Lock cachedSourcesLock = new ReentrantLock();
    private Object destObject;
    private File cachedDest;
    private transient Lock cachedDestLock = new ReentrantLock();
    private boolean quiet = false;
    private boolean overwrite = true;
    private boolean onlyIfModified = false;
    private boolean compress = true;
    private String username;
    private String password;
    private boolean preemptiveAuth = false;
    private Map<String, String> headers;
    private boolean acceptAnyCertificate = false;
    private int connectTimeoutMs = 30 * 1000;
    private int readTimeoutMs = 30 * 1000;
    private int retries = 0;
    private File downloadTaskDir;
    private boolean tempAndMove = false;
    private UseETag useETag = UseETag.FALSE;
    private File cachedETagsFile;
    private transient Lock cachedETagsFileLock = new ReentrantLock();
    private final AtomicInteger upToDate = new AtomicInteger(0);

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
        this.projectLayout = project.getLayout();
        this.logger = project.getLogger();
        if (task != null) {
            this.servicesOwner = task;
        } else {
            this.servicesOwner = project;
        }
        this.objectFactory = project.getObjects();
        this.isOffline = project.getGradle().getStartParameter().isOffline();
        this.downloadTaskDir = new File(project.getBuildDir(), "download-task");
    }

    /**
     * Starts downloading
     * @return a {@link CompletableFuture} that completes once the download
     * has finished
     * @throws IOException if the file could not downloaded
     */
    public CompletableFuture<Void> execute() throws IOException {
        return execute(true);
    }

    /**
     * Starts downloading
     * @param throwOnError {@code true} if the asynchronous worker action should
     * throw if the download fails. {@code false} if only the returned
     * {@link CompletableFuture} should complete exceptionally.
     * @return a {@link CompletableFuture} that completes once the download
     * has finished
     * @throws IOException if the file could not downloaded
     */
    public CompletableFuture<Void> execute(boolean throwOnError) throws IOException {
        if (GradleVersion.current().compareTo(HARD_MIN_GRADLE_VERSION) < 0 && !quiet) {
            throw new IllegalStateException("gradle-download-task requires " +
                    "Gradle 5.x or higher");
        }
        // if (GradleVersion.current().compareTo(SOFT_MIN_GRADLE_VERSION) < 0 && !quiet) {
        //     logger.warn("Support for running gradle-download-task "
        //             + "with Gradle 2.x, 3.x, and 4.x has been deprecated and will be removed in "
        //             + "gradle-download-task 5.0.0");
        // }
        if (JavaVersion.current().compareTo(JavaVersion.VERSION_1_8) < 0 && !quiet) {
            throw new IllegalStateException("gradle-download-task requires " +
                    "Java 8 or higher");
        }
        // if (JavaVersion.current().compareTo(JavaVersion.VERSION_1_8) < 0 && !quiet) {
        //     logger.warn("Support for running gradle-download-task "
        //             + "using Java 7 has been deprecated and will be removed in "
        //             + "gradle-download-task 5.0.0");
        // }

        if (sourceObjects.isEmpty()) {
            throw new IllegalArgumentException("Please provide a download source");
        }
        if (destObject == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }

        List<URL> sources = getSources();
        File dest = getDest();

        if (dest.equals(projectLayout.getBuildDirectory().get().getAsFile())) {
            //make sure build dir exists
            dest.mkdirs();
        }

        if (sources.size() > 1 && !dest.isDirectory()) {
            if (!dest.exists()) {
                // create directory automatically
                dest.mkdirs();
            } else {
                throw new IllegalArgumentException("If multiple sources are provided, "
                        + "the destination has to be a directory.");
            }
        }

        WorkerExecutorHelper workerExecutor = WorkerExecutorHelper.newInstance(objectFactory);

        CachingHttpClientFactory clientFactory = new CachingHttpClientFactory();
        CompletableFuture<?>[] futures = new CompletableFuture[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            URL src = sources.get(i);

            // create progress logger
            ProgressLoggerWrapper progressLogger = new ProgressLoggerWrapper(logger);
            if (!quiet) {
                try {
                    progressLogger.init(servicesOwner, src.toString());
                } catch (Exception e) {
                    // unable to get progress logger
                    logger.error("Unable to get progress logger. Download "
                            + "progress will not be displayed.");
                }
            }

            // submit download job for asynchronous execution
            CompletableFuture<Void> f = new CompletableFuture<>();
            futures[i] = f;
            workerExecutor.submit(() -> {
                try {
                    execute(src, clientFactory, progressLogger);
                    f.complete(null);
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                    if (throwOnError) {
                        throw t;
                    }
                }
            });
        }

        // wait for all downloads to finish (necessary if we're on an old
        // Gradle version (< 5.6) without Worker API)
        if (workerExecutor.needsAwait()) {
            workerExecutor.await();
        }

        // Create a custom completable future that calls `workerExecutor.await()`
        // on any `get` call. This is necessary so we can wait for all work
        // items in the queue even if we only have one thread (i.e. if
        // `max-workers` equals 1). See issue #205 for more details.
        CompletableFuture<Void> rf = new WorkerExecutorFuture(workerExecutor);

        CompletableFuture.allOf(futures).whenComplete((v, t) -> {
            // always close HTTP client factory
            try {
                clientFactory.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (t == null) {
                rf.complete(v);
            } else {
                rf.completeExceptionally(t);
            }
        });

        return rf;
    }

    private void execute(URL src, HttpClientFactory clientFactory,
            ProgressLoggerWrapper progressLogger) throws IOException {
        final File destFile = makeDestFile(src);
        if (!overwrite && destFile.exists()) {
            if (!quiet) {
                logger.info("Destination file already exists. "
                        + "Skipping '" + destFile.getName() + "'");
            }
            upToDate.incrementAndGet();
            return;
        }

        progressLogger.setDestFileName(destFile.getName());

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
        
        if ("file".equals(src.getProtocol())) {
            executeFileProtocol(src, timestamp, destFile, progressLogger);
        } else {
            executeHttpProtocol(src, clientFactory, timestamp, destFile, progressLogger);
        }
    }

    private void executeFileProtocol(URL src, long timestamp, File destFile,
            ProgressLoggerWrapper progressLogger) throws IOException {
        File srcFile = null;
        try {
            srcFile = new File(src.toURI());
            progressLogger.setSize(srcFile.length());
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
                upToDate.incrementAndGet();
                return;
            }
        }

        BufferedInputStream fileStream = new BufferedInputStream(src.openStream());
        streamAndMove(fileStream, destFile, progressLogger);
        
        //set last-modified time of destination file
        if (onlyIfModified && lastModified > 0) {
            destFile.setLastModified(lastModified);
        }
    }

    private void executeHttpProtocol(URL src, HttpClientFactory clientFactory,
            long timestamp, File destFile, ProgressLoggerWrapper progressLogger)
            throws IOException {
        //create HTTP host from URL
        HttpHost httpHost = new HttpHost(src.getProtocol(), src.getHost(), src.getPort());
        
        //create HTTP client
        CloseableHttpClient client = clientFactory.createHttpClient(
                httpHost, acceptAnyCertificate, retries, logger, quiet);

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
            int code = response.getCode();
            if (code == HttpStatus.SC_NOT_MODIFIED ||
                    (lastModified != 0 && timestamp >= lastModified)) {
                if (!quiet) {
                    logger.info("Not modified. Skipping '" + src + "'");
                }
                upToDate.incrementAndGet();
                return;
            }
        
            //perform the download
            performDownload(response, destFile, progressLogger);
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
     * @param progressLogger progress logger
     * @throws IOException if the response could not be downloaded
     */
    private void performDownload(CloseableHttpResponse response, File destFile,
            ProgressLoggerWrapper progressLogger) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return;
        }

        // get content length
        long contentLength = entity.getContentLength();
        if (contentLength >= 0) {
            progressLogger.setSize(contentLength);
        }

        // open stream and start downloading
        InputStream is = entity.getContent();
        streamAndMove(is, destFile, progressLogger);
    }

    /**
     * Move a file by calling {@link File#renameTo(File)} first and, if this
     * fails, by copying and deleting it.
     * @param src the file to move
     * @param dest the destination
     * @param progressLogger progress logger
     * @throws IOException if the file could not be moved
     */
    private void moveFile(File src, File dest,
            ProgressLoggerWrapper progressLogger) throws IOException {
        if (src.renameTo(dest)) {
            return;
        }

        // renameTo() failed. Try to copy the file and delete it afterwards.
        // see issue #146
        try (InputStream is = new FileInputStream(src)) {
            stream(is, dest, progressLogger);
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
     * {@link #stream(InputStream, File, ProgressLoggerWrapper)}.
     * @param is the input stream to read
     * @param destFile the destination file
     * @param progressLogger progress logger
     * @throws IOException if an I/O error occurs
     */
    private void streamAndMove(InputStream is, File destFile,
            ProgressLoggerWrapper progressLogger) throws IOException {
        if (!tempAndMove) {
            stream(is, destFile, progressLogger);
        } else {
            //create parent directory
            downloadTaskDir.mkdirs();

            //create name of temporary file
            File tempFile = File.createTempFile(destFile.getName(), ".part",
                    downloadTaskDir);

            //stream and move
            stream(is, tempFile, progressLogger);
            if (destFile.exists()) {
                //Delete destFile if it exists before renaming tempFile.
                //Otherwise renaming might fail.
                if (!destFile.delete()) {
                    throw new IOException("Could not delete old destination file '" +
                            destFile.getAbsolutePath() + "'.");
                }
            }
            try {
                moveFile(tempFile, destFile, progressLogger);
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
     * @param progressLogger progress logger
     * @throws IOException if an I/O error occurs
     */
    private void stream(InputStream is, File destFile,
            ProgressLoggerWrapper progressLogger) throws IOException {
        try {
            progressLogger.started();

            boolean finished = false;
            try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    destFile.toPath(), StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE)) {
                long pos = 0;
                Future<Integer> writeFuture = null;

                byte[] buf1 = new byte[1024 * 10];
                byte[] buf2 = new byte[1024 * 10];
                ByteBuffer bb1 = ByteBuffer.wrap(buf1);
                ByteBuffer bb2 = ByteBuffer.wrap(buf2);

                int read;
                while ((read = is.read(buf1)) >= 0) {
                    if (writeFuture != null) {
                        writeFuture.get();
                    }
                    bb1.position(0);
                    bb1.limit(read);
                    writeFuture = channel.write(bb1, pos);
                    pos += read;
                    progressLogger.incrementProgress(read);

                    // swap buffers for next asynchronous operation
                    byte[] tmpBuf = buf1;
                    buf1 = buf2;
                    buf2 = tmpBuf;
                    ByteBuffer tmpBB = bb1;
                    bb1 = bb2;
                    bb2 = tmpBB;
                }
                if (writeFuture != null) {
                    writeFuture.get();
                }

                finished = true;
            } catch (InterruptedException e) {
                throw new IOException("Writing to destination file was interrupted", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException)e.getCause();
                }
                throw new IOException("Could not write to destination file", e);
            } finally {
                if (!finished) {
                    destFile.delete();
                }
            }
        } finally {
            is.close();
            progressLogger.completed();
        }
    }

    /**
     * Reads the {@link #cachedETagsFile}
     * @return a map containing the parsed contents of the cached etags file
     * or an empty map if the file does not exist yet
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readCachedETags() {
        cachedETagsFileLock.lock();
        try {
            File cachedETagsFile = getCachedETagsFile();
            Map<String, Object> cachedETags;
            if (cachedETagsFile.exists()) {
                JsonSlurper slurper = new JsonSlurper();
                cachedETags = (Map<String, Object>)slurper.parse(cachedETagsFile, "UTF-8");
            } else {
                cachedETags = new LinkedHashMap<>();
            }
            return cachedETags;
        } finally {
            cachedETagsFileLock.unlock();
        }
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

        cachedETagsFileLock.lock();
        try {
            // create directory for cached etags file
            File parent = getCachedETagsFile().getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            // read existing cached etags file
            Map<String, Object> cachedETags = readCachedETags();

            // create new entry in cached ETags file
            Map<String, String> etagMap = new LinkedHashMap<>();
            etagMap.put("ETag", etag);

            String uri = host.toURI();
            Map<String, Object> hostMap = (Map<String, Object>)cachedETags.get(uri);
            if (hostMap == null) {
                hostMap = new LinkedHashMap<>();
                cachedETags.put(uri, hostMap);
            }
            hostMap.put(file, etagMap);

            // write cached ETags file
            String cachedETagsContents = JsonOutput.toJson(cachedETags);
            try (PrintWriter writer = new PrintWriter(getCachedETagsFile(), "UTF-8")) {
                writer.write(cachedETagsContents);
                writer.flush();
            }
        } finally {
            cachedETagsFileLock.unlock();
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
        // configure authentication
        HttpClientContext context = null;
        if (username != null && password != null) {
            context = HttpClientContext.create();
            Credentials c = new UsernamePasswordCredentials(username, password.toCharArray());
            addAuthentication(httpHost, c, context);
        }
        
        //create request
        HttpGet get = new HttpGet(file);

        //configure timeouts
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
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
            HttpHost proxy = new HttpHost(scheme, proxyHost, nProxyPort);
            Credentials credentials = new UsernamePasswordCredentials(
                    proxyUser, proxyPassword.toCharArray());
            addAuthentication(proxy, credentials, context);
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
        int code = response.getCode();
        if ((code < 200 || code > 299) && code != HttpStatus.SC_NOT_MODIFIED) {
            String url = httpHost + file;
            String message = "HTTP status code: " + code + ", URL: " + url;
            if (code == HttpStatus.SC_UNAUTHORIZED && !response.containsHeader(HttpHeaders.WWW_AUTHENTICATE)) {
                message += ". Missing " + HttpHeaders.WWW_AUTHENTICATE + " header in response; use the " +
                        "preemptiveAuth flag to send credentials in first request";
            }
            String phrase = response.getReasonPhrase();
            if (phrase == null || phrase.isEmpty()) {
                phrase = message;
            } else {
                phrase += " (" + message + ")";
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
     * @param context the context in which the authentication information
     * should be saved
     */
    private void addAuthentication(HttpHost host, Credentials credentials,
            HttpClientContext context) {
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

        if (preemptiveAuth && username != null && password != null) {
            BasicScheme basicAuth = new BasicScheme();
            basicAuth.initPreemptive(credentials);
            authCache.put(host, basicAuth);
        }

        ((CredentialsStore)credsProvider).setCredentials(new AuthScope(host), credentials);
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
    
    /**
     * @return true if the download destination is up to date
     */
    public boolean isUpToDate() {
        return upToDate.get() == getSources().size();
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
    public void preemptiveAuth(boolean preemptiveAuth) {
        this.preemptiveAuth = preemptiveAuth;
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
        if (dir instanceof Function0) {
            // lazily evaluate Kotlin function
            Function0<?> function = (Function0<?>)dir;
            dir = function.invoke();
        }
        if (dir instanceof Closure) {
            // lazily evaluate closure
            Closure<?> closure = (Closure<?>)dir;
            dir = closure.call();
        }
        if (dir instanceof Provider) {
            dir = ((Provider<?>)dir).getOrNull();
        }
        if (dir instanceof CharSequence) {
            return projectLayout.getProjectDirectory().file(dir.toString()).getAsFile();
        } else if (dir instanceof Directory) {
            File f = ((Directory)dir).getAsFile();

            // Make sure the directory exists so we actually download to a file
            // inside this directory. Otherwise, we will just create a file
            // with the name of this directory.
            f.mkdirs();

            return f;
        } else if (dir instanceof RegularFile) {
            return ((RegularFile)dir).getAsFile();
        } else if (dir instanceof File) {
            return (File)dir;
        }

        return null;
    }

    @Override
    public void downloadTaskDir(Object dir) {
        downloadTaskDir = getDestinationFromDirProperty(dir);
        if (downloadTaskDir == null) {
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
        if (location instanceof Function0) {
            // lazily evaluate Kotlin function
            Function0<?> function = (Function0<?>)location;
            location = function.invoke();
        }
        if (location instanceof Closure) {
            // lazily evaluate closure
            Closure<?> closure = (Closure<?>)location;
            location = closure.call();
        }
        if (location instanceof Provider) {
            location = ((Provider<?>)location).getOrNull();
        }
        if (location instanceof CharSequence) {
            this.cachedETagsFile = projectLayout.getProjectDirectory()
                    .file(location.toString()).getAsFile();
        } else if (location instanceof RegularFile) {
            this.cachedETagsFile = ((RegularFile)location).getAsFile();
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

        if (src instanceof Function0) {
            // lazily evaluate Kotlin function
            Function0<?> function = (Function0<?>)src;
            src = function.invoke();
        }
        if (src instanceof Closure) {
            // lazily evaluate closure
            Closure<?> closure = (Closure<?>)src;
            src = closure.call();
        }
        if (src instanceof Provider) {
            src = ((Provider<?>)src).getOrNull();
        }
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
        cachedSourcesLock.lock();
        try {
            if (cachedSources != null) {
                return cachedSources;
            }

            cachedSources = new ArrayList<>(sourceObjects.size());
            for (Object src : sourceObjects) {
                cachedSources.addAll(convertSource(src));
            }

            return cachedSources;
        } finally {
            cachedSourcesLock.unlock();
        }
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
        cachedDestLock.lock();
        try {
            if (cachedDest != null) {
                return cachedDest;
            }

            cachedDest = getDestinationFromDirProperty(destObject);
            if (cachedDest == null) {
                throw new IllegalArgumentException("Download destination must " +
                        "be one of a File, Directory, RegularFile, or a CharSequence");
            }

            return cachedDest;
        } finally {
            cachedDestLock.unlock();
        }
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
    public boolean isPreemptiveAuth() {
        return preemptiveAuth;
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
        cachedETagsFileLock.lock();
        try {
            if (cachedETagsFile == null) {
                return new File(this.downloadTaskDir, "etags.json");
            }
            return cachedETagsFile;
        } finally {
            cachedETagsFileLock.unlock();
        }
    }

    /**
     * In order to support Gradle's configuration cache, we need to make some
     * fields transient. This method re-initializes these fields after the
     * object has been read from the cache.
     * @param in the input stream to read the object from the cache
     * @throws IOException if the object could not be read
     * @throws ClassNotFoundException if the object could not be deserialized
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // initialize transient fields
        cachedSourcesLock = new ReentrantLock();
        cachedDestLock = new ReentrantLock();
        cachedETagsFileLock = new ReentrantLock();
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

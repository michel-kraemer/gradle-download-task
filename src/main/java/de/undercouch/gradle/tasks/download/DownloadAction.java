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

package de.undercouch.gradle.tasks.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.gradle.api.Project;

import de.undercouch.gradle.tasks.download.internal.ContentEncodingNoneInterceptor;
import de.undercouch.gradle.tasks.download.internal.InsecureHostnameVerifier;
import de.undercouch.gradle.tasks.download.internal.InsecureTrustManager;
import de.undercouch.gradle.tasks.download.internal.ProgressLoggerWrapper;
import groovy.lang.Closure;

/**
 * Downloads a file and displays progress
 * @author Michel Kraemer
 */
public class DownloadAction implements DownloadSpec {
    private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = new InsecureHostnameVerifier();
    private static final TrustManager[] INSECURE_TRUST_MANAGERS = { new InsecureTrustManager() };
    
    private final Project project;
    private List<URL> sources = new ArrayList<URL>(1);
    private File dest;
    private boolean quiet = false;
    private boolean overwrite = true;
    private boolean onlyIfNewer = false;
    private boolean compress = true;
    private String username;
    private String password;
    private AuthScheme authScheme;
    private Credentials credentials;
    private Map<String, String> headers;
    private boolean acceptAnyCertificate = false;

    private ProgressLoggerWrapper progressLogger;
    private String size;
    private long processedBytes = 0;
    private long loggedKb = 0;

    private int upToDate = 0;
    private int skipped = 0;

    private SSLConnectionSocketFactory insecureSSLSocketFactory = null;
    
    /**
     * Creates a new download action
     * @param project the project to be built
     */
    public DownloadAction(Project project) {
        this.project = project;
    }

    /**
     * Starts downloading
     * @throws IOException if the file could not downloaded
     */
    public void execute() throws IOException {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Please provide a download source");
        }
        if (dest == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }

        if (dest.equals(project.getBuildDir())) {
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
        
        for (URL src : sources) {
            execute(src);
        }
    }

    private void execute(URL src) throws IOException {
        final File destFile = makeDestFile(src);
        if (!overwrite && destFile.exists()) {
            if (!quiet) {
                project.getLogger().info("Destination file already exists. "
                        + "Skipping '" + destFile.getName() + "'");
            }
            ++upToDate;
            return;
        }
        
        // in case offline mode is enabled don't try to download if
        // destination already exists
        if (project.getGradle().getStartParameter().isOffline()) {
            if (destFile.exists()) {
                if (!quiet) {
                    project.getLogger().info("Skipping existing file '" +
                            destFile.getName() + "' in offline mode.");
                }
                ++skipped;
                return;
            }
            throw new IllegalStateException("Unable to download " + src +
                    " in offline mode.");
        }

        final long timestamp = onlyIfNewer && destFile.exists() ? destFile.lastModified() : 0;
        
        //create progress logger
        if (!quiet) {
            try {
                progressLogger = new ProgressLoggerWrapper(project, src.toString());
            } catch (Exception e) {
                //unable to get progress logger
                project.getLogger().error("Unable to get progress logger. Download "
                        + "progress will not be displayed.");
            }
        }
        
        //create HTTP host from URL
        HttpHost httpHost = new HttpHost(src.getHost(), src.getPort(), src.getProtocol());
        
        //create HTTP client
        CloseableHttpClient client = createHttpClient(httpHost);
        
        try {
            //open URL connection
            CloseableHttpResponse response = openConnection(httpHost, src.getFile(),
                    timestamp, client);
            //check if file on server was modified
            long lastModified = parseLastModified(response);
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_NOT_MODIFIED ||
                    (lastModified != 0 && timestamp >= lastModified)) {
                if (!quiet) {
                    project.getLogger().info("Not modified. Skipping '" + src + "'");
                }
                ++upToDate;
                return;
            }
            
            //perform the download
            try {
                performDownload(response, destFile);
            } finally {
                response.close();
            }
        } finally {
            client.close();
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
        try {
            startProgress();
            OutputStream os = new FileOutputStream(destFile);
            
            boolean finished = false;
            try {
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
                os.close();
                if (!finished) {
                    destFile.delete();
                }
            }
        } finally {
            is.close();
            completeProgress();
        }
        
        long newTimestamp = parseLastModified(response);
        if (onlyIfNewer && newTimestamp > 0) {
            destFile.setLastModified(newTimestamp);
        }
    }

    /**
     * Generates the path to an output file for a given source URL. Creates
     * all necessary parent directories for the destination file.
     * @param src the source
     * @return the path to the output file
     */
    private File makeDestFile(URL src) {
        if (dest == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }

        File destFile = dest;
        if (destFile.isDirectory()) {
            //guess name from URL
            String name = src.toString();
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            name = name.substring(name.lastIndexOf('/') + 1);
            destFile = new File(dest, name);
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
     * Configure proxy for a given HTTP host
     * @param httpHost the HTTP host
     * @return the proxy or <code>null</code> if not proxy is necessary
     * @throws IOException if the proxy could not be configured
     */
    private HttpHost configureProxy(HttpHost httpHost) throws IOException {
        HttpHost proxy = null;
        
        String scheme = httpHost.getSchemeName();
        if (!"http".equals(scheme) && !"https".equals(scheme) &&
                !"ftp".equals(scheme)) {
            return proxy;
        }
        
        String host = System.getProperty(scheme + ".proxyHost");
        if (host != null) {
            String portStr = System.getProperty(scheme + ".proxyPort");
            if (portStr != null) {
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal proxy port: " + portStr);
                }
                proxy = new HttpHost(host, port);
            } else {
                proxy = new HttpHost(host);
            }
        }
        
        return proxy;
    }
    
    /**
     * Creates an HTTP client for the given host
     * @param httpHost the host to connect to
     * @return the HTTP client
     */
    private CloseableHttpClient createHttpClient(HttpHost httpHost) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        
        //accept any certificate if necessary
        if ("https".equals(httpHost.getSchemeName()) && acceptAnyCertificate) {
            SSLConnectionSocketFactory icsf = getInsecureSSLSocketFactory();
            builder.setSSLSocketFactory(icsf);
            Registry<ConnectionSocketFactory> registry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", icsf)
                        .build();
            HttpClientConnectionManager cm =
                    new BasicHttpClientConnectionManager(registry);
            builder.setConnectionManager(cm);
        }
        
        //add an interceptor that replaces the invalid Content-Type
        //'none' by 'identity'
        builder.addInterceptorFirst(new ContentEncodingNoneInterceptor());

        CloseableHttpClient client = builder.build();
        return client;
    }
    
    /**
     * Opens a connection to the given HTTP host and requests a file. Checks
     * the last-modified header on the server if the given timestamp is
     * greater than 0.
     * @param httpHost the HTTP host to connect to
     * @param file the file to request
     * @param timestamp the timestamp of the destination file, in milliseconds
     * @param client the HTTP client to use to perform the request
     * @return the URLConnection
     * @throws IOException if the connection could not be opened
     */
    private CloseableHttpResponse openConnection(HttpHost httpHost, String file,
            long timestamp, CloseableHttpClient client) throws IOException {
        //perform preemptive authentication
        HttpClientContext context = null;
        if ((username != null && password != null) || credentials != null) {
            context = HttpClientContext.create();
            AuthScheme as = authScheme;
            if (as == null) {
                as = new BasicScheme();
            }
            Credentials c;
            if (username != null && password != null) {
                if (!(as instanceof BasicScheme) && !(as instanceof DigestScheme)) {
                    throw new IllegalArgumentException("If 'username' and "
                            + "'password' are set 'authScheme' must be either "
                            + "'Basic' or 'Digest'.");
                }
                c = new UsernamePasswordCredentials(username, password);
            } else {
                c = credentials;
            }
            addAuthentication(httpHost, c, as, context);
        }
        
        //create request
        HttpGet get = new HttpGet(file);
        
        //configure proxy
        HttpHost proxy = configureProxy(httpHost);
        if (proxy != null) {
            RequestConfig config = RequestConfig.custom()
                .setProxy(proxy)
                .build();
            get.setConfig(config);
            
            //add authentication information for proxy
            String scheme = httpHost.getSchemeName();
            String proxyUser = System.getProperty(scheme + ".proxyUser");
            String proxyPassword = System.getProperty(scheme + ".proxyPassword");
            if (proxyUser != null && proxyPassword != null) {
                if (context == null) {
                    context = HttpClientContext.create();
                }
                Credentials credentials =
                        new UsernamePasswordCredentials(proxyUser, proxyPassword);
                addAuthentication(proxy, credentials, null, context);
            }
        }
        
        //set If-Modified-Since header
        if (timestamp > 0) {
            get.setHeader("If-Modified-Since", DateUtils.formatDate(new Date(timestamp)));
        }
        
        //set headers
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                get.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        
        //enable compression
        if (compress) {
            get.setHeader("Accept-Encoding", "gzip");
        }
        
        //execute request
        CloseableHttpResponse response = client.execute(httpHost, get, context);
        
        //handle response
        int code = response.getStatusLine().getStatusCode();
        if ((code < 200 || code > 299) && code != HttpStatus.SC_NOT_MODIFIED) {
            throw new ClientProtocolException(response.getStatusLine().getReasonPhrase());
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
        return upToDate == sources.size();
    }
    
    /**
     * @return true if execution of this task has been skipped
     */
    public boolean isSkipped() {
        return skipped == sources.size();
    }

    /**
     * @return a list of files created by this action (i.e. the destination files)
     */
    public List<File> getOutputFiles() {
        List<File> files = new ArrayList<File>(sources.size());
        for (URL src : sources) {
            files.add(makeDestFile(src));
        }
        return files;
    }
    
    @Override
    public void src(Object src) throws MalformedURLException {
        if (src instanceof Closure) {
            //lazily evaluate closure
            Closure<?> closure = (Closure<?>)src;
            src = closure.call();
        }
        
        if (src instanceof CharSequence) {
            sources.add(new URL(src.toString()));
        } else if (src instanceof URL) {
            sources.add((URL)src);
        } else if (src instanceof Collection) {
            Collection<?> sc = (Collection<?>)src;
            for (Object sco : sc) {
                src(sco);
            }
        } else if (src != null && src.getClass().isArray()) {
            int len = Array.getLength(src);
            for (int i = 0; i < len; ++i) {
                Object sco = Array.get(src, i);
                src(sco);
            }
        } else {
            throw new IllegalArgumentException("Download source must " +
                "either be a URL, a CharSequence, a Collection or an array.");
        }
    }
    
    @Override
    public void dest(Object dest) {
        if (dest instanceof Closure) {
            //lazily evaluate closure
            Closure<?> closure = (Closure<?>)dest;
            dest = closure.call();
        }
        
        if (dest instanceof CharSequence) {
            this.dest = project.file(dest.toString());
        } else if (dest instanceof File) {
            this.dest = (File)dest;
        } else {
            throw new IllegalArgumentException("Download destination must " +
                "either be a File or a CharSequence");
        }
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
    public void onlyIfNewer(boolean onlyIfNewer) {
        this.onlyIfNewer = onlyIfNewer;
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
    public void authScheme(Object authScheme) {
        if (authScheme instanceof AuthScheme) {
            this.authScheme = (AuthScheme)authScheme;
        } else if (authScheme instanceof String) {
            String sa = (String)authScheme;
            if (sa.equalsIgnoreCase("Basic")) {
                this.authScheme = new BasicScheme();
            } else if (sa.equalsIgnoreCase("Digest")) {
                this.authScheme = new DigestScheme();
            } else {
                throw new IllegalArgumentException("Invalid authentication scheme: "
                        + "'" + sa + "'. Valid values are 'Basic' and 'Digest'.");
            }
        } else {
            throw new IllegalArgumentException("Invalid authentication "
                    + "scheme. Provide either a String or an instance of "
                    + AuthScheme.class.getName() + ".");
        }
    }

    @Override
    public void credentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void headers(Map<String, String> headers) {
        if (this.headers == null) {
            this.headers = new LinkedHashMap<String, String>();
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
            headers = new LinkedHashMap<String, String>();
        }
        headers.put(name, value);
    }

    @Override
    public void acceptAnyCertificate(boolean accept) {
        this.acceptAnyCertificate = accept;
    }

    @Override
    public Object getSrc() {
        if (sources.size() == 1) {
            return sources.get(0);
        }
        return sources;
    }
    
    @Override
    public File getDest() {
        return dest;
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
    public boolean isOnlyIfNewer() {
        return onlyIfNewer;
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
    public AuthScheme getAuthScheme() {
        return authScheme;
    }
    
    @Override
    public Credentials getCredentials() {
        if (credentials != null) {
            return credentials;
        } else if (username != null && password != null) {
            return new UsernamePasswordCredentials(username, password);
        }
        return null;
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

    private SSLConnectionSocketFactory getInsecureSSLSocketFactory() {
        if (insecureSSLSocketFactory == null) {
            SSLContext sc;
            try {
                sc = SSLContext.getInstance("SSL");
                sc.init(null, INSECURE_TRUST_MANAGERS, new SecureRandom());
                insecureSSLSocketFactory = new SSLConnectionSocketFactory(
                        sc, INSECURE_HOSTNAME_VERIFIER);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
        return insecureSSLSocketFactory;
    }
}

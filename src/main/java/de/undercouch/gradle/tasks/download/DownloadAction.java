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
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.tools.ant.util.Base64Converter;
import org.gradle.api.Project;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;

import de.undercouch.gradle.tasks.download.internal.InsecureHostnameVerifier;
import de.undercouch.gradle.tasks.download.internal.InsecureTrustManager;
import groovy.lang.Closure;

/**
 * Downloads a file and displays progress
 * @author Michel Kraemer
 */
public class DownloadAction implements DownloadSpec {
    private static final int MAX_NUMBER_OF_REDIRECTS = 30;
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
    private Map<String, String> headers;
    private boolean acceptAnyCertificate = false;

    private ProgressLogger progressLogger;
    private String size;
    private long processedBytes = 0;
    private long loggedKb = 0;

    private int upToDate = 0;
    private int skipped = 0;

    private SSLSocketFactory insecureSSLSocketFactory = null;
    
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
        if (sources == null || sources.isEmpty()) {
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
            throw new IllegalArgumentException("If multiple sources are provided "
                    + "the destination has to be a directory.");
        }
        
        for (URL src : sources) {
            execute(src);
        }
    }
    
    private void execute(URL src) throws IOException {
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
        
        long timestamp = 0;
        if (onlyIfNewer && destFile.exists()) {
            timestamp = destFile.lastModified();
        }
        
        //create progress logger
        if (!quiet) {
            //we are about to access an internal class. Use reflection here to provide
            //as much compatibility to different Gradle versions as possible
            try {
                Method getServices = project.getClass().getMethod("getServices");
                Object serviceFactory = getServices.invoke(project);
                Method get = serviceFactory.getClass().getMethod("get", Class.class);
                Object progressLoggerFactory = get.invoke(serviceFactory,
                        ProgressLoggerFactory.class);
                Method newOperation = progressLoggerFactory.getClass().getMethod(
                        "newOperation", Class.class);
                progressLogger = (ProgressLogger)newOperation.invoke(
                        progressLoggerFactory, getClass());
                String desc = "Download " + src.toString();
                Method setDescription = progressLogger.getClass().getMethod(
                        "setDescription", String.class);
                setDescription.setAccessible(true);
                setDescription.invoke(progressLogger, desc);
                Method setLoggingHeader = progressLogger.getClass().getMethod(
                        "setLoggingHeader", String.class);
                setLoggingHeader.setAccessible(true);
                setLoggingHeader.invoke(progressLogger, desc);
            } catch (Exception e) {
                //unable to get progress logger
                project.getLogger().error("Unable to get progress logger. Download "
                        + "progress will not be displayed.");
            }
        }
        
        //open URL connection
        URLConnection conn = openConnection(src, timestamp);
        if (conn == null) {
            return;
        }
        
        //get content length
        long contentLength = parseContentLength(conn);
        if (contentLength >= 0) {
            size = toLengthText(contentLength);
        }
        
        processedBytes = 0;
        loggedKb = 0;
        
        //open stream and start downloading
        InputStream is = conn.getInputStream();
        if (isContentCompressed(conn)) {
            is = new GZIPInputStream(is);
        }
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
        
        long newTimestamp = conn.getLastModified();
        if (onlyIfNewer && newTimestamp > 0) {
            destFile.setLastModified(newTimestamp);
        }
    }
    
    /**
     * Opens a URLConnection. Checks the last-modified header on the
     * server if the given timestamp is greater than 0.
     * @param src the source URL to open a connection for
     * @param timestamp the timestamp of the destination file
     * @param project the project to be built
     * @return the URLConnection or null if the download should be skipped
     * @throws IOException if the connection could not be opened
     */
    private URLConnection openConnection(URL src, long timestamp) throws IOException {
        int redirects = MAX_NUMBER_OF_REDIRECTS;
        
        URLConnection uc = src.openConnection();
        while (true) {
            if (uc instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection)uc;
                httpConnection.setInstanceFollowRedirects(true);
            }

            if (uc instanceof HttpsURLConnection && acceptAnyCertificate) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection)uc;
                httpsConnection.setSSLSocketFactory(getInsecureSSLSocketFactory());
                httpsConnection.setHostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
            }
            
            //set If-Modified-Since header
            if (timestamp > 0) {
                uc.setIfModifiedSince(timestamp);
            }
            
            //authenticate
            if (username != null && password != null) {
                String up = username + ":" + password;
                Base64Converter encoder = new Base64Converter();
                String encoding = encoder.encode(up.getBytes());
                uc.setRequestProperty("Authorization", "Basic " + encoding);
            }

            //set headers
            if (headers != null) {
                for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                    uc.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
            }
            
            //enable compression
            if (compress) {
                uc.setRequestProperty("Accept-Encoding", "gzip");
            }
            
            uc.connect();
    
            if (uc instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection)uc;
                int responseCode = httpConnection.getResponseCode();
                long lastModified = httpConnection.getLastModified();
                if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED ||
                        (lastModified != 0 && timestamp >= lastModified)) {
                    if (!quiet) {
                        project.getLogger().info("Not modified. Skipping '" + src + "'");
                    }
                    ++upToDate;
                    return null;
                }
                
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    if (redirects == 0) {
                        throw new IllegalStateException("Request exceeds maximum number "
                                + "of redirects (" + MAX_NUMBER_OF_REDIRECTS + ")");
                    }

                    //redirect to other location and try again
                    String nu = uc.getHeaderField("Location");
                    String cookie = uc.getHeaderField("Set-Cookie");
                    uc = (HttpURLConnection)new URL(nu).openConnection();
                    uc.setRequestProperty("Cookie", cookie);
                    
                    redirects--;
                    continue;
                }
            }
            
            break;
        }

        return uc;
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
     * Parses the Content-Length of a {@link URLConnection}. Use this
     * method to workaround the fact that {@link URLConnection#getContentLength()}
     * only returns an <code>int</code> whereas this method returns
     * a <code>long</code>
     * @param conn the {@link URLConnection}
     * @return the content length or -1 if it is unknown
     */
    private long parseContentLength(URLConnection conn) {
        String value = conn.getHeaderField("Content-Length");
        if (value == null || value.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Checks if the content of the given URL connection is compressed
     * @return true if it is compressed, false otherwise
     */
    private boolean isContentCompressed(URLConnection conn) {
        String value = conn.getHeaderField("Content-Encoding");
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.equalsIgnoreCase("gzip");
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
    boolean isUpToDate() {
        return sources != null && upToDate == sources.size();
    }
    
    /**
     * @return true if execution of this task has been skipped
     */
    boolean isSkipped() {
        return sources != null && skipped == sources.size();
    }
    
    @Override
    public void src(Object src) throws MalformedURLException {
        if (sources == null) {
            sources = new ArrayList<URL>(1);
        }
        
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
    public void headers(Map<String, String> headers) {
        if (headers == null) {
            headers = new LinkedHashMap<String, String>();
        }
        this.headers.putAll(headers);
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
        if (sources != null && sources.size() == 1) {
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

    private SSLSocketFactory getInsecureSSLSocketFactory() {
        if (insecureSSLSocketFactory == null) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, INSECURE_TRUST_MANAGERS, new SecureRandom());
                insecureSSLSocketFactory = sc.getSocketFactory();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
        return insecureSSLSocketFactory;
    }
}

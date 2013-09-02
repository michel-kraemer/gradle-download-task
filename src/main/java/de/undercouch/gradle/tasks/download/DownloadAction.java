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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.tools.ant.util.Base64Converter;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;

/**
 * Downloads a file and displays progress
 * @author Michel Kraemer
 */
public class DownloadAction implements DownloadSpec {
    private URL src;
    private File dest;
    private boolean quiet = false;
    private boolean overwrite = true;
    private boolean onlyIfNewer = false;
    private String username;
    private String password;
    
    private ProgressLogger progressLogger;
    private String size;
    private long processedBytes = 0;
    private long loggedKb = 0;
    
    /**
     * Starts downloading
     * @param project the project to be built
     * @throws IOException if the file could not downloaded
     */
    public void execute(Project project) throws IOException {
        if (src == null) {
            throw new IllegalArgumentException("Please provide a download source");
        }
        if (dest == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }
        
        if (dest.equals(project.getBuildDir())) {
            //make sure build dir exists
            dest.mkdirs();
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
        
        if (!overwrite && destFile.exists()) {
            if (!quiet) {
                project.getLogger().info("Destination file already exists. "
                        + "Skipping '" + destFile.getName() + "'");
            }
            return;
        }
        
        long timestamp = 0;
        if (onlyIfNewer && destFile.exists()) {
            timestamp = destFile.lastModified();
        }
        
        //create progress logger
        if (!quiet) {
            if (project instanceof ProjectInternal) {
                ProjectInternal pi = (ProjectInternal)project;
                ProgressLoggerFactory plf = pi.getServices().get(ProgressLoggerFactory.class);
                progressLogger = plf.newOperation(getClass());
                String desc = "Download " + src.toString();
                progressLogger.setDescription(desc);
                progressLogger.setLoggingHeader(desc);
            }
        }
        
        //open URL connection
        URLConnection conn = openConnection(timestamp, project);
        if (conn == null) {
            return;
        }
        
        //get content length
        long contentLength = parseContentLength(conn);
        if (contentLength >= 0) {
            size = toLengthText(contentLength);
        }
        
        //open stream and start downloading
        InputStream is = conn.getInputStream();
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
     * @param timestamp the timestamp of the destination file
     * @param project the project to be built
     * @return the URLConnection or null if the download should be skipped
     * @throws IOException if the connection could not be opened
     */
    private URLConnection openConnection(long timestamp,
            Project project) throws IOException {
        URLConnection uc = src.openConnection();
        
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
                return null;
            }
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
    
    @Override
    public void src(Object src) throws MalformedURLException {
        if (src instanceof String) {
            this.src = new URL((String)src);
        } else if (src instanceof URL) {
            this.src = (URL)src;
        } else {
            throw new IllegalArgumentException("Download source must " +
                "either be a URL or a String");
        }
    }
    
    @Override
    public void dest(Object dest) {
        if (dest instanceof String) {
            this.dest = new File((String)dest);
        } else if (dest instanceof File) {
            this.dest = (File)dest;
        } else {
            throw new IllegalArgumentException("Download destination must " +
                "either be a File or a String");
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
    public void username(String username) {
        this.username = username;
    }
    
    @Override
    public void password(String password) {
        this.password = password;
    }
    
    @Override
    public URL getSrc() {
        return src;
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
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
}

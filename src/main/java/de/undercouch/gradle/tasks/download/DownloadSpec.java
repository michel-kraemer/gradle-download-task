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
import java.net.MalformedURLException;
import java.util.Map;

/**
 * An interface for classes that perform file downloads
 * @author Michel Kraemer
 */
public interface DownloadSpec {
    /**
     * Sets the download source URL
     * @param src the URL
     * @throws MalformedURLException if the download source is not a URL
     */
    void src(Object src) throws MalformedURLException;
    
    /**
     * Sets the download destination
     * @param dest a file or directory where to store the retrieved file
     */
    void dest(Object dest);
    
    /**
     * Sets the quiet flag
     * @param quiet true if download progress should not be logged
     */
    void quiet(boolean quiet);
    
    /**
     * Sets the overwrite flag
     * @param overwrite true if existing files should be overwritten, false otherwise
     */
    void overwrite(boolean overwrite);
    
    /**
     * Sets the onlyIfModified flag
     * @param onlyIfModified true if the file should only be downloaded if it
     * has been modified on the server since the last download
     */
    void onlyIfModified(boolean onlyIfModified);
    
    /**
     * Sets the onlyIfNewer flag. This method is an alias for
     * {@link #onlyIfModified(boolean)}.
     * @param onlyIfNewer true if the file should only be downloaded if it
     * has been modified on the server since the last download
     */
    void onlyIfNewer(boolean onlyIfNewer);
    
    /**
     * Specifies if compression should be used during download
     * @param compress true if compression should be enabled
     */
    void compress(boolean compress);

    /**
     * Sets the username for authentication
     * @param username the username
     */
    void username(String username);

    /**
     * Sets the password for authentication
     * @param password the password
     */
    void password(String password);

    /**
     * Sets the HTTP request headers to use when downloading
     * @param headers a Map of header names to values
     */
    void headers(Map<String, String> headers);

    /**
     * Sets an HTTP request header to use when downloading
     * @param name name of the HTTP header
     * @param value value of the HTTP header
     */
    void header(String name, String value);

    /**
     * Specifies if preemptive Basic authentication should be enabled. By default,
     * gradle-download-task automatically detects the required authentication
     * scheme by sending two requests: one without credentials to determine
     * the scheme based on the {@code WWW-Authenticate} header in the server's
     * response and the actual request with credentials. This will fail if the
     * server does not send a {@code WWW-Authenticate} header. In this case,
     * set {@code preemptiveAuth} to {@code true} to use Basic authentication
     * and to always send credentials in the first request.
     *
     * Note: Sending credentials in clear text in the first request without
     * checking if the server actually needs them might pose a security risk.
     *
     * @param preemptiveAuth {@code true} if preemptive Basic authentication
     * should be enabled
     */
    void preemptiveAuth(boolean preemptiveAuth);

    /**
     * Specifies if HTTPS certificate verification errors should be ignored
     * and any certificate (even an invalid one) should be accepted. By default
     * certificates are validated and errors are not being ignored.
     * @param accept true if certificate errors should be ignored (default: false)
     */
    void acceptAnyCertificate(boolean accept);

    /**
     * Specifies the maximum time to wait in milliseconds until a connection is
     * established. A value of zero means infinite timeout. A negative value
     * is interpreted as undefined.
     * @param milliseconds the timeout in milliseconds (default: -1)
     */
    void connectTimeout(int milliseconds);

    /**
     * Specifies the maximum time in milliseconds to wait for data from the
     * server. A value of zero means infinite timeout. A negative value is
     * interpreted as undefined.
     * @param milliseconds the timeout in milliseconds (default: -1)
     */
    void readTimeout(int milliseconds);

    /**
     * Specifies the maximum number of retry attempts if a request has failed.
     * By default, requests are never retried and the task fails immediately if
     * the first request does not succeed.
     * @param retries the maximum number of retries (default: 0)
     */
    void retries(int retries);

    /**
     * Specifies the directory where gradle-download-task stores information
     * that should persist between builds
     * @param dir the directory (default: ${buildDir}/gradle-download-task)
     */
    void downloadTaskDir(Object dir);

    /**
     * Specifies whether the file should be downloaded to a temporary location
     * and, upon successful execution, moved to the final location. If the
     * overwrite flag is set to false, this flag is useful to avoid partially
     * downloaded files if Gradle is forcefully closed or the system crashes.
     * Note that the plugin always deletes partial downloads on connection
     * errors, regardless of the value of this flag. The default temporary
     * location can be configured with the {@link #downloadTaskDir(Object)};
     * @param tempAndMove true if the file should be downloaded to a temporary
     * location and, upon successful execution, moved to the final location
     * (default: false)
     */
    void tempAndMove(boolean tempAndMove);

    /**
     * <p>Sets the <code>useETag</code> flag. Possible values are:</p>
     * <ul>
     * <li><code>true</code>: check if the entity tag (ETag) of a downloaded
     * file has changed and issue a warning if a weak ETag was encountered</li>
     * <li><code>false</code>: Do not use entity tags (ETags) at all</li>
     * <li><code>"all"</code>: Use all ETags but do not issue a warning for weak ones</li>
     * <li><code>"strongOnly"</code>: Use only strong ETags</li>
     * </ul>
     * <p>Note that this flag is only effective if <code>onlyIfModified</code> is
     * <code>true</code>.</p>
     * @param useETag the flag's new value
     */
    void useETag(Object useETag);

    /**
     * Sets the location of the file that keeps entity tags (ETags) received
     * from the server
     * @param location the location (default: ${downloadTaskDir}/etags.json)
     */
    void cachedETagsFile(Object location);

    /**
     * @return the download source(s), either a URL or a list of URLs
     */
    Object getSrc();
    
    /**
     * @return the download destination
     */
    File getDest();
    
    /**
     * @return the quiet flag
     */
    boolean isQuiet();
    
    /**
     * @return the overwrite flag
     */
    boolean isOverwrite();
    
    /**
     * @return the onlyIfModified flag
     */
    boolean isOnlyIfModified();
    
    /**
     * Get the <code>onlyIfNewer</code> flag. This method is an alias for
     * {@link #isOnlyIfModified()}.
     * @return the onlyIfNewer flag
     */
    boolean isOnlyIfNewer();
    
    /**
     * @return true if compression is enabled
     */
    boolean isCompress();

    /**
     * @return the username for authentication
     */
    String getUsername();

    /**
     * @return the password for authentication
     */
    String getPassword();

    /**
     * @return true if preemptive Basic authenticate is enabled
     * @see #preemptiveAuth(boolean)
     */
    boolean isPreemptiveAuth();

    /**
     * @return the HTTP request headers to use when downloading
     */
    Map<String, String> getHeaders();

    /**
     * @param name name of the HTTP header
     * @return the value of the HTTP header
     */
    String getHeader(String name);

    /**
     * @return true if HTTPS certificate verification errors should be ignored,
     * default value is false
     */
    boolean isAcceptAnyCertificate();

    /**
     * @return the maximum time to wait in milliseconds until a connection
     * is established
     */
    int getConnectTimeout();

    /**
     * @return the maximum time in milliseconds to wait for data from the
     * server
     */
    int getReadTimeout();

    /**
     * @return the maximum number of retries
     */
    int getRetries();
    
    /**
     * @return the directory where gradle-download-task stores information
     * that should persist between builds
     */
    File getDownloadTaskDir();

    /**
     * @return true of if the file should be downloaded to a temporary
     * location and, upon successful execution, moved to the final
     * location.
     */
    boolean isTempAndMove();

    /**
     * @return the value of the <code>useETag</code> flag
     * @see #useETag(Object)
     */
    Object getUseETag();

    /**
     * @return the location of the file that keeps entity tags (ETags) received
     * from the server
     */
    File getCachedETagsFile();
}

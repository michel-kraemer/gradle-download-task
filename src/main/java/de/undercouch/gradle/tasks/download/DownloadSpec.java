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
import java.net.URL;

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
     * Sets the onlyIfNewer flag
     * @param onlyIfNewer true if the file should only be downloaded if it
     * has been modified on the server since the last download
     */
    void onlyIfNewer(boolean onlyIfNewer);
    
    /**
     * Sets the username for <code>Basic</code> authentication
     * @param username the username
     */
    void username(String username);
    
    /**
     * Sets the password for <code>Basic</code> authentication
     * @param password the password
     */
    void password(String password);
    
    /**
     * @return the download source
     */
    URL getSrc();
    
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
     * @return the onlyIfNewer flag
     */
    boolean isOnlyIfNewer();
    
    /**
     * @return the username for <code>Basic</code> authentication
     */
    String getUsername();
    
    /**
     * @return the password for <code>Basic</code> authentication
     */
    String getPassword();
}

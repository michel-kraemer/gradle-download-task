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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Downloads a file and displays progress. Example:
 * <pre>
 * task downloadFile(type: Download) {
 *     src 'http://www.example.com/file.ext'
 *     dest buildDir
 * }
 * </pre>
 * @author Michel Kraemer
 */
public class Download extends DefaultTask implements DownloadSpec {
    private final DownloadAction action;

    /**
     * Default constructor
     */
    public Download() {
        // get required project properties now to enable configuration cache
        final boolean isOffline = getProject().getGradle().getStartParameter().isOffline();

        action = new DownloadAction(getProject(), this);

        getOutputs().upToDateWhen(task -> !(isOnlyIfModified() || isOverwrite()));
        
        onlyIf(task -> {
            // in case offline mode is enabled don't try to download if
            // destination already exists
            if (isOffline) {
                for (File f : getOutputFiles()) {
                    if (f.exists()) {
                        if (!isQuiet()) {
                            getProject().getLogger().info("Skipping existing file '" +
                                    f.getName() + "' in offline mode.");
                        }
                    } else {
                        throw new IllegalStateException("Unable to download file '" +
                                f.getName() + "' in offline mode.");
                    }
                }
                return false;
            }
            return true;
        });
    }
    
    /**
     * Starts downloading
     * @throws IOException if the file could not downloaded
     */
    @TaskAction
    public void download() throws IOException {
        action.execute().thenRun(() -> {
            // handle 'upToDate'
            if (action.isUpToDate()) {
                getState().setDidWork(false);
            }
        });
    }

    /**
     * @return a list of files created by this task (i.e. the destination files)
     */
    @OutputFiles
    public List<File> getOutputFiles() {
        return action.getOutputFiles();
    }
    
    @Override
    public void src(Object src) {
        action.src(src);
    }
    
    @Override
    public void dest(Object dest) {
        action.dest(dest);
    }
    
    @Override
    public void quiet(boolean quiet) {
        action.quiet(quiet);
    }
    
    @Override
    public void overwrite(boolean overwrite) {
        action.overwrite(overwrite);
    }
    
    @Override
    public void onlyIfModified(boolean onlyIfModified) {
        action.onlyIfModified(onlyIfModified);
    }
    
    @Override
    public void onlyIfNewer(boolean onlyIfNewer) {
        action.onlyIfNewer(onlyIfNewer);
    }
    
    @Override
    public void compress(boolean compress) {
        action.compress(compress);
    }
    
    @Override
    public void username(String username) {
        action.username(username);
    }
    
    @Override
    public void password(String password) {
        action.password(password);
    }

    @Override
    public void preemptiveAuth(boolean preemptiveAuth) {
        action.preemptiveAuth(preemptiveAuth);
    }

    @Override
    public void headers(Map<String, String> headers) {
        action.headers(headers);
    }

    @Override
    public void header(String name, String value) {
        action.header(name, value);
    }

    @Override
    public void acceptAnyCertificate(boolean accept) {
        action.acceptAnyCertificate(accept);
    }

    @Override
    public void connectTimeout(int milliseconds) {
        action.connectTimeout(milliseconds);
    }

    @Override
    public void readTimeout(int milliseconds) {
        action.readTimeout(milliseconds);
    }

    @Override
    public void retries(int retries) {
        action.retries(retries);
    }

    @Override
    public void downloadTaskDir(Object dir) {
        action.downloadTaskDir(dir);
    }

    @Override
    public void tempAndMove(boolean tempAndMove) {
        action.tempAndMove(tempAndMove);
    }

    @Override
    public void useETag(Object useETag) {
        action.useETag(useETag);
    }

    @Override
    public void cachedETagsFile(Object location) {
        action.cachedETagsFile(location);
    }

    @Input
    @Override
    public Object getSrc() {
        return action.getSrc();
    }

    @Internal // see #getOutputFiles()
    @Override
    public File getDest() {
        return action.getDest();
    }

    @Console
    @Override
    public boolean isQuiet() {
        return action.isQuiet();
    }

    @Input
    @Override
    public boolean isOverwrite() {
        return action.isOverwrite();
    }

    @Input
    @Override
    public boolean isOnlyIfModified() {
        return action.isOnlyIfModified();
    }

    @Input
    @Override
    public boolean isOnlyIfNewer() {
        return action.isOnlyIfNewer();
    }

    @Input
    @Override
    public boolean isCompress() {
        return action.isCompress();
    }

    @Input
    @Optional
    @Override
    public String getUsername() {
        return action.getUsername();
    }

    @Input
    @Optional
    @Override
    public String getPassword() {
        return action.getPassword();
    }

    @Input
    @Override
    public boolean isPreemptiveAuth() {
        return action.isPreemptiveAuth();
    }

    @Input
    @Optional
    @Override
    public Map<String, String> getHeaders() {
        return action.getHeaders();
    }

    @Override
    public String getHeader(String name) {
        return action.getHeader(name);
    }

    @Input
    @Override
    public boolean isAcceptAnyCertificate() {
        return action.isAcceptAnyCertificate();
    }

    @Input
    @Override
    public int getConnectTimeout() {
        return action.getConnectTimeout();
    }

    @Input
    @Override
    public int getReadTimeout() {
        return action.getReadTimeout();
    }

    @Input
    @Override
    public int getRetries() {
        return action.getRetries();
    }

    @Internal
    @Override
    public File getDownloadTaskDir() {
        return action.getDownloadTaskDir();
    }

    @Input
    @Override
    public boolean isTempAndMove() {
        return action.isTempAndMove();
    }

    @Input
    @Optional
    @Override
    public Object getUseETag() {
        return action.getUseETag();
    }

    @Internal
    @Override
    public File getCachedETagsFile() {
        return action.getCachedETagsFile();
    }
}

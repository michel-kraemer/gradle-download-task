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
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.Credentials;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

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
        action = new DownloadAction(getProject());
        getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                return !(isOnlyIfNewer() || isOverwrite());
            }
        });
    }
    
    /**
     * Starts downloading
     * @throws IOException if the file could not downloaded
     */
    @TaskAction
    public void download() throws IOException {
        action.execute();
        
        // handle 'skipped' and 'upToDate'
        try {
            if (action.isSkipped()) {
                //we are about to access an internal class. Use reflection here to provide
                //as much compatibility to previous Gradle versions as possible (see issue #16)
                Method getState = this.getClass().getMethod("getState");
                Object state = getState.invoke(this);
                Method skipped = state.getClass().getMethod("skipped", String.class);
                if (skipped != null) {
                    skipped.invoke(state, "Download skipped");
                }
            } else if (action.isUpToDate()) {
                Method getState = this.getClass().getMethod("getState");
                Object state = getState.invoke(this);
                Method upToDate = state.getClass().getMethod("upToDate");
                if (upToDate != null) {
                    upToDate.invoke(state);
                }
            }
        } catch (Exception e) {
            //just ignore
        }
    }

    /**
     * @return a list of files created by this task (i.e. the destination files)
     */
    @OutputFiles
    public List<File> getOutputFiles() {
        return action.getOutputFiles();
    }
    
    @Override
    public void src(Object src) throws MalformedURLException {
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
    public void authScheme(Object authScheme) {
        action.authScheme(authScheme);
    }

    @Override
    public void credentials(Credentials credentials) {
        action.credentials(credentials);
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
    public Object getSrc() {
        return action.getSrc();
    }
    
    @Override
    public File getDest() {
        return action.getDest();
    }
    
    @Override
    public boolean isQuiet() {
        return action.isQuiet();
    }
    
    @Override
    public boolean isOverwrite() {
        return action.isOverwrite();
    }
    
    @Override
    public boolean isOnlyIfNewer() {
        return action.isOnlyIfNewer();
    }
    
    @Override
    public boolean isCompress() {
        return action.isCompress();
    }
    
    @Override
    public String getUsername() {
        return action.getUsername();
    }
    
    @Override
    public String getPassword() {
        return action.getPassword();
    }
    
    @Override
    public AuthScheme getAuthScheme() {
        return action.getAuthScheme();
    }

    @Override
    public Credentials getCredentials() {
        return action.getCredentials();
    }

    @Override
    public Map<String, String> getHeaders() {
        return action.getHeaders();
    }

    @Override
    public String getHeader(String name) {
        return action.getHeader(name);
    }

    @Override
    public boolean isAcceptAnyCertificate() {
        return action.isAcceptAnyCertificate();
    }
}

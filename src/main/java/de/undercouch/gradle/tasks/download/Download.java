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

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.Credentials;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
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
        action = new DownloadAction(getProject());
        getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                return !(isOnlyIfModified() || isOverwrite());
            }
        });

        onlyIf(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                // in case offline mode is enabled don't try to download if
                // destination already exists
                if (getProject().getGradle().getStartParameter().isOffline()) {
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
            }
        });
    }

    /**
     * Starts downloading
     * @throws IOException if the file could not downloaded
     */
    @TaskAction
    public void download() throws IOException, NoSuchAlgorithmException {
        action.execute();

        // handle 'upToDate'
        try {
            if (action.isUpToDate()) {
                Method getState = this.getClass().getMethod("getState");
                Object state = getState.invoke(this);
                try {
                    // prior to Gradle 3.2 we could do this
                    Method upToDate = state.getClass().getMethod("upToDate");
                    upToDate.invoke(state);
                } catch (NoSuchMethodException e) {
                    // since Gradle 3.2 we need to do this
                    setUpToDate(state);
                }
            }
        } catch (Exception e) {
            //just ignore
        }
    }

    /**
     * Set the task's outcome to UP_TO_DATE
     * @param state the task's state
     * @throws ClassNotFoundException if the class 'TaskExecutionOutcome' was not found
     * @throws NoSuchMethodException if one of the methods to set the outcome was not found
     * @throws InvocationTargetException if the outcome could not be set
     * @throws IllegalAccessException if the outcome could not be set
     */
    private void setUpToDate(Object state) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // get TaskExecutionOutput.UP_TO_DATE
        Class<?> TaskExecutionOutcome = Class.forName(
                "org.gradle.api.internal.tasks.TaskExecutionOutcome");
        Method valueOf = TaskExecutionOutcome.getMethod(
                "valueOf", String.class);
        Object UP_TO_DATE = valueOf.invoke(null, "UP_TO_DATE");

        // set outcome
        Method setOutcome = state.getClass().getMethod(
                "setOutcome", TaskExecutionOutcome);
        setOutcome.invoke(state, UP_TO_DATE);

        // pretend we did not do anything
        Method setDidWork = state.getClass().getMethod(
                "setDidWork", boolean.class);
        setDidWork.invoke(state, false);
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
    public void timeout(int milliseconds) {
        action.timeout(milliseconds);
    }

    @Override
    public void requestInterceptor(HttpRequestInterceptor interceptor) {
        action.requestInterceptor(interceptor);
    }

    @Override
    public void responseInterceptor(HttpResponseInterceptor interceptor) {
        action.responseInterceptor(interceptor);
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
    public boolean isOnlyIfModified() {
        return action.isOnlyIfModified();
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

    @Override
    public int getTimeout() {
        return action.getTimeout();
    }

    @Override
    public HttpRequestInterceptor getRequestInterceptor() {
        return action.getRequestInterceptor();
    }

    @Override
    public HttpResponseInterceptor getResponseInterceptor() {
        return action.getResponseInterceptor();
    }

    @Override
    public void algorithm(String algorithm) {
        action.algorithm(algorithm);
    }

    @Override
    public void checksum(String checksum) {
        action.checksum(checksum);
    }

    @Override
    public String getAlgorithm() {
        return action.getAlgorithm();
    }

    @Override
    public String getChecksum() {
        return action.getChecksum();
    }
}

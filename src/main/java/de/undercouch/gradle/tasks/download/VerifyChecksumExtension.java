// Copyright 2017 ThoughtWorks, Inc
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

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * An extension that configures and executes a {@link DownloadAction}
 * @author Ketan Padegaonkar
 */
public class VerifyChecksumExtension implements Configurable<VerifyChecksumExtension> {
    private Project project;

    /**
     * Creates a new extension
     * @param project the project to be built
     */
    public VerifyChecksumExtension(Project project) {
        this.project = project;
    }

    @Override
    public VerifyChecksumExtension configure(@SuppressWarnings("rawtypes") Closure cl) {
        VerifyAction action = ConfigureUtil.configure(cl, new VerifyAction(project));
        try {
            action.execute();
        } catch (IOException e) {
            throw new IllegalStateException("Could not download file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid algorithm specified", e);
        }
        return this;
    }
}

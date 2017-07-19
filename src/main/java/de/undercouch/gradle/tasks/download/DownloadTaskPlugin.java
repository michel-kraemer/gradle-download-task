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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers the extensions provided by this plugin
 * @author Michel Kraemer
 */
public class DownloadTaskPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("download", DownloadExtension.class, project);
        project.getExtensions().create("verifyChecksum", VerifyChecksumExtension.class, project);
    }
}

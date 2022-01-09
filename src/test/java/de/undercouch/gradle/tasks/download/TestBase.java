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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for unit tests
 * @author Michel Kraemer
 */
public abstract class TestBase {
    protected static final String TEST_FILE_NAME = "test.txt";
    protected static final String CONTENTS = "Hello world";
    protected static final String TEST_FILE_NAME2 = "test2.txt";
    protected static final String CONTENTS2 = "Elvis lives!";

    /**
     * A temporary directory where a virtual test project is stored
     */
    protected File projectDir;

    /**
     * A folder for temporary files
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Set up the unit tests
     * @throws Exception if anything goes wrong
     */
    @Before
    public void setUp() throws Exception {
        projectDir = folder.newFolder("project");
    }
    
    /**
     * Makes a Gradle project and creates a download task
     * @return the unconfigured download task
     */
    protected Download makeProjectAndTask() {
        return makeProjectAndTask(null);
    }

    protected Download makeProjectAndTask(@Nullable Action<Project> projectConfiguration) {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();

        if (projectConfiguration != null) {
            projectConfiguration.execute(project);
        }
        
        Map<String, Object> applyParams = new HashMap<>();
        applyParams.put("plugin", "de.undercouch.download");
        project.apply(applyParams);
        
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", Download.class);
        return (Download)project.task(taskParams, "downloadFile");
    }

    protected void execute(Task t) {
        List<Action<? super Task>> actions = t.getActions();
        for (Action<? super Task> a : actions) {
            a.execute(t);
        }
    }
}

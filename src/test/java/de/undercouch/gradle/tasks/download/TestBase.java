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

import de.undercouch.gradle.tasks.download.internal.WorkerExecutorHelper;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.internal.operations.BuildOperationCategory;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationState;
import org.gradle.internal.operations.CurrentBuildOperationRef;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public Path folder;

    /**
     * Set up the unit tests
     */
    @BeforeEach
    public void setUp(@TempDir Path tempDir) {
        folder = tempDir;
        projectDir = new File(folder.toFile(), "project");
    }

    protected File newTempFile() throws IOException {
        return File.createTempFile("gradle-download-task", null, folder.toFile());
    }

    protected File newTempDir() throws IOException {
        Path result = Files.createTempDirectory(folder, "gradle-download-task");
        return result.toFile();
    }

    /**
     * Makes a Gradle project and creates a download task
     * @return the unconfigured download task
     */
    protected Download makeProjectAndTask() {
        return makeProjectAndTask(null);
    }

    protected Download makeProjectAndTask(@Nullable Action<Project> projectConfiguration) {
        Project project = makeProject(projectConfiguration);
        return makeTask(project);
    }

    protected Download makeTask(Project project) {
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("type", Download.class);

        // start parent build operation
        BuildOperationState op = new BuildOperationState(
                BuildOperationDescriptor.displayName("")
                        .metadata(BuildOperationCategory.TASK)
                        .build(), 0);
        op.setRunning(true);
        CurrentBuildOperationRef.instance().set(op);

        return (Download)project.task(taskParams, "downloadFile");
    }

    protected Project makeProject() {
        return makeProject(null);
    }

    protected Project makeProject(@Nullable Action<Project> projectConfiguration) {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();

        if (projectConfiguration != null) {
            projectConfiguration.execute(project);
        }

        Map<String, Object> applyParams = new HashMap<>();
        applyParams.put("plugin", "de.undercouch.download");
        project.apply(applyParams);

        return project;
    }

    protected void execute(Task t) {
        List<Action<? super Task>> actions = t.getActions();
        for (Action<? super Task> a : actions) {
            a.execute(t);
        }
        WorkerExecutorHelper.newInstance(t.getProject().getObjects()).await();
    }
}

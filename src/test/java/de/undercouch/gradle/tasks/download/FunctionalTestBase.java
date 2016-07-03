// Copyright 2013-2016 Michel Kraemer
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

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Base class for functional tests
 * @author Jan Berkel
 */
public abstract class FunctionalTestBase extends TestBase {
    /**
     * A temporary folder for test files
     */
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File buildFile;

    /**
     * Set up the functional tests
     * @throws Exception if anything went wrong
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        buildFile = testProjectDir.newFile("build.gradle");
    }

    /**
     * Create a gradle runner using the given build file
     * @param buildFile the build file
     * @param debug run test in debugger
     * @return the gradle runner
     * @throws IOException if the build file could not written to disk
     */
    protected GradleRunner createRunnerWithBuildFile(String buildFile,
            boolean debug) throws IOException {
        writeBuildFile(buildFile);
        return GradleRunner.create()
            .withPluginClasspath()
            .withDebug(debug)
            .withProjectDir(testProjectDir.getRoot());
    }

    /**
     * Asserts that a given task was successful
     * @param task the task
     */
    protected void assertTaskSuccess(BuildTask task) {
        assertNotNull("task is null", task);
        assertEquals("task " + task + " state should be success", SUCCESS, task.getOutcome());
    }

    /**
     * Asserts that a given task has been marked as up-to-date
     * @param task the task
     */
    protected void assertTaskUpToDate(BuildTask task) {
        assertNotNull("task is null", task);
        assertEquals("task " + task + " state should be up-to-date", UP_TO_DATE, task.getOutcome());
    }

    /**
     * Asserts that a given task has been marked as skipped
     * @param task the task
     */
    protected void assertTaskSkipped(BuildTask task) {
        assertNotNull("task is null", task);
        assertEquals("task " + task + " state should be skipped", SKIPPED, task.getOutcome());
    }

    /**
     * Write a gradle build file to disk
     * @param content the build file's contents
     * @throws IOException if the file could not be written
     */
    private void writeBuildFile(String content) throws IOException {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(buildFile));
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}

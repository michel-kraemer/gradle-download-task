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

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

/**
 * Base class for functional tests
 * @author Jan Berkel
 */
public abstract class FunctionalTestBase extends TestBaseWithMockServer {
    /**
     * A temporary folder for test files
     */
    public Path testProjectDir;

    private File buildFile;
    private File propertiesFile;

    /**
     * Set up the functional tests
     */
    @BeforeEach
    public void setUpFunctionalTests(@TempDir Path testProjectDir) {
        this.testProjectDir = testProjectDir;
        buildFile = new File(testProjectDir.toFile(), "build.gradle");
        propertiesFile = new File(testProjectDir.toFile(), "gradle.properties");
    }

    /**
     * Create a gradle runner using the given build file
     * @param buildFile the build file
     * @param gradleVersion the Gradle version to test against
     * @return the gradle runner
     * @throws IOException if the build file could not written to disk
     */
    protected GradleRunner createRunnerWithBuildFile(String buildFile,
            String gradleVersion) throws IOException {
        FileUtils.writeStringToFile(this.buildFile, buildFile, StandardCharsets.UTF_8);

        writePropertiesFile();

        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withGradleVersion(gradleVersion);
    }

    /**
     * Asserts that a given task was successful
     * @param task the task
     */
    protected void assertTaskSuccess(BuildTask task) {
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).isSameAs(SUCCESS);
    }

    /**
     * Asserts that a given task has been marked as up-to-date
     * @param task the task
     */
    protected void assertTaskUpToDate(BuildTask task) {
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).isSameAs(UP_TO_DATE);
    }

    /**
     * Asserts that a given task has been marked as skipped
     * @param task the task
     */
    protected void assertTaskSkipped(BuildTask task) {
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).isSameAs(SKIPPED);
    }

    /**
     * Write a default 'gradle.properties' file to disk
     * @throws IOException if the file could not be written
     */
    private void writePropertiesFile() throws IOException {
        // stop gradle daemon immediately and set maximum heap size to
        // a low value so the functional tests run well on the CI server
        String content = "org.gradle.daemon.idletimeout=0\n" +
                "org.gradle.jvmargs=-Xmx128M\n";

        FileUtils.writeStringToFile(propertiesFile, content, StandardCharsets.UTF_8);
    }
}

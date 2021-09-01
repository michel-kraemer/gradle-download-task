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

//import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
//import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
//import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//import org.apache.commons.io.FileUtils;
//import org.gradle.testkit.runner.BuildTask;
//import org.gradle.testkit.runner.GradleRunner;
//import org.junit.Rule;
//import org.junit.rules.TemporaryFolder;
//
///**
// * Base class for functional tests
// * @author Jan Berkel
// */
//public abstract class FunctionalTestBase extends TestBaseWithMockServer {
//    /**
//     * A temporary folder for test files
//     */
//    @Rule
//    public final TemporaryFolder testProjectDir = new TemporaryFolder();
//
//    /**
//     * The version of Gradle to run the test with, null for default.
//     */
//    protected String gradleVersion;
//
//    private File buildFile;
//    private File propertiesFile;
//
//    /**
//     * Set up the functional tests
//     * @throws Exception if anything went wrong
//     */
//    @Override
//    public void setUp() throws Exception {
//        super.setUp();
//        buildFile = testProjectDir.newFile("build.gradle");
//        propertiesFile = testProjectDir.newFile("gradle.properties");
//    }
//
//    /**
//     * Create a gradle runner using the given build file
//     * @param buildFile the build file
//     * @return the gradle runner
//     * @throws IOException if the build file could not written to disk
//     */
//    protected GradleRunner createRunnerWithBuildFile(String buildFile) throws IOException {
//        FileUtils.writeStringToFile(this.buildFile, buildFile, StandardCharsets.UTF_8);
//
//        writePropertiesFile();
//        GradleRunner runner = GradleRunner.create()
//            .withPluginClasspath()
//            .withProjectDir(testProjectDir.getRoot());
//        if (gradleVersion != null) {
//            runner = runner.withGradleVersion(gradleVersion);
//        }
//        return runner;
//    }
//
//    /**
//     * Asserts that a given task was successful
//     * @param task the task
//     */
//    protected void assertTaskSuccess(BuildTask task) {
//        assertNotNull("task is null", task);
//        assertEquals("task " + task + " state should be success", SUCCESS, task.getOutcome());
//    }
//
//    /**
//     * Asserts that a given task has been marked as up-to-date
//     * @param task the task
//     */
//    protected void assertTaskUpToDate(BuildTask task) {
//        assertNotNull("task is null", task);
//        assertEquals("task " + task + " state should be up-to-date", UP_TO_DATE, task.getOutcome());
//    }
//
//    /**
//     * Asserts that a given task has been marked as skipped
//     * @param task the task
//     */
//    protected void assertTaskSkipped(BuildTask task) {
//        assertNotNull("task is null", task);
//        assertEquals("task " + task + " state should be skipped", SKIPPED, task.getOutcome());
//    }
//
//    /**
//     * Write a default 'gradle.properties' file to disk
//     * @throws IOException if the file could not be written
//     */
//    private void writePropertiesFile() throws IOException {
//        // stop gradle daemon immediately and set maximum heap size to
//        // a low value so the functional tests run well on the CI server
//        String content = "org.gradle.daemon.idletimeout=0\n" +
//                "org.gradle.jvmargs=-Xmx128M\n";
//
//        FileUtils.writeStringToFile(propertiesFile, content, StandardCharsets.UTF_8);
//    }
//}

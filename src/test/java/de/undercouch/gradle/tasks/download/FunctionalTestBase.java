package de.undercouch.gradle.tasks.download;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.assertEquals;

public abstract class FunctionalTestBase extends TestBase {
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File buildFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        buildFile = testProjectDir.newFile("build.gradle");
    }

    protected GradleRunner createRunnerWithBuildFile(String buildFile) throws IOException {
        writeBuildFile(buildFile);
        return GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.getRoot());
    }

    protected void assertTaskSuccess(BuildTask task) {
        assertEquals("task "+task+" state should be success", SUCCESS, task.getOutcome());
    }

    protected void assertTaskUpToDate(BuildTask task) {
        assertEquals("task "+task+" state should be up-to-date", UP_TO_DATE, task.getOutcome());
    }

    protected void assertTaskSkipped(BuildTask task) {
        assertEquals("task "+task+" state should be skipped", SKIPPED, task.getOutcome());
    }

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

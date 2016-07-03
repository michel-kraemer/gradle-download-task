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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

/**
 * Tests the plugin's functionality
 * @author Jan Berkel
 */
public class FunctionalDownloadTest extends FunctionalTestBase {
    private String singleSrc;
    private String multipleSrc;
    private String dest;
    private File destFile;

    /**
     * Set up the functional tests
     * @throws Exception if anything went wrong
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        singleSrc = "'" + makeSrc(TEST_FILE_NAME) + "'";
        multipleSrc = "['" +  makeSrc(TEST_FILE_NAME) + "', '" + makeSrc(TEST_FILE_NAME2) + "']";
        destFile = new File(testProjectDir.getRoot(), "someFile");
        dest = "file('" + destFile.getName() + "')";
    }

    /**
     * Test if a single file can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        assertTrue(destFile.exists());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(destFile));
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileWithQuietMode() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false, true, false, true)));
        assertTrue(destFile.exists());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(destFile));
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileWithoutCompress() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false, false, false, false)));
        assertTrue(destFile.exists());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(destFile));
    }

    /**
     * Test if multiple files can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        assertTaskSuccess(download(new Parameters(multipleSrc, dest, true, false)));
        assertTrue(destFile.isDirectory());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(
                new File(destFile, TEST_FILE_NAME)));
        assertArrayEquals(contents2, FileUtils.readFileToByteArray(
                new File(destFile, TEST_FILE_NAME2)));
    }

    /**
     * Download a file twice and check if the second attempt is skipped
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceMarksTaskAsUpToDate() throws Exception {
        final Parameters parameters = new Parameters(singleSrc, dest, false, false);
        assertTaskSuccess(download(parameters));
        assertTaskUpToDate(download(parameters));
    }

    /**
     * Download a file with 'overwrite' flag and check if the second attempt succeeds
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceWithOverwriteExecutesTwice() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
    }

    /**
     * Download a file twice in offline mode and check if the second attempt is
     * skipped even if the 'overwrite' flag is set
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceWithOfflineMode() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        assertTaskSkipped(download(new Parameters(singleSrc, dest, true, false, true, true, false)));
    }

    /**
     * Download a file once, then download again with 'onlyIfNewer'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewer() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true)));
    }

    /**
     * Download a file once, then download again with 'onlyIfNewer'.
     * File changed between downloads.
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewerRedownloadsIfFileHasBeenUpdated() throws Exception {
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        File src = new File(folder.getRoot(), TEST_FILE_NAME);
        assertTrue(src.setLastModified(src.lastModified() + 5000));
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, true)));
    }

    /**
     * Create destination file locally, then run download.
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewerReDownloadIfFileExists() throws Exception {
        File testFile = new File(folder.getRoot(), TEST_FILE_NAME);
        FileUtils.writeByteArrayToFile(destFile, contents);
        assertTrue(destFile.setLastModified(testFile.lastModified()));
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
    }

    /**
     * Test if the download task is triggered if another task depends on its
     * output files
     * @throws Exception if anything went wrong
     */
    @Test
    public void fileDependenciesTriggersDownloadTask() throws Exception {
        assertTaskSuccess(runTask(":processTask", new Parameters(singleSrc, dest, true, false)));
        assertTrue(destFile.exists());
    }

    /**
     * Create a download task
     * @param parameters the download parameters
     * @return the download task
     * @throws Exception if anything went wrong
     */
    protected BuildTask download(Parameters parameters) throws Exception {
        return runTask(":downloadTask", parameters);
    }

    /**
     * Create a task
     * @param taskName the task's name
     * @param parameters the download parameters
     * @return the task
     * @throws Exception if anything went wrong
     */
    protected BuildTask runTask(String taskName, Parameters parameters) throws Exception {
        return createRunner(parameters)
                .withArguments(parameters.offline ? asList("--offline", taskName) :
                    singletonList(taskName))
                .build()
                .task(taskName);
    }

    /**
     * Create a gradle runner to test against
     * @param parameters the download parameters
     * @return the runner
     * @throws IOException if the build file could not be created
     */
    protected GradleRunner createRunner(Parameters parameters) throws IOException {
        return createRunnerWithBuildFile(
            "plugins { id 'de.undercouch.download' }\n" +
            "task downloadTask(type: de.undercouch.gradle.tasks.download.Download) { \n" +
                "src(" + parameters.src + ")\n" +
                "dest " + parameters.dest + "\n" +
                "overwrite " + Boolean.toString(parameters.overwrite) + "\n" +
                "onlyIfNewer " + Boolean.toString(parameters.onlyIfNewer) + "\n" +
                "compress " + Boolean.toString(parameters.compress) + "\n" +
                "quiet " + Boolean.toString(parameters.quiet) + "\n" +
            "}\n" +
            "task processTask {\n" +
                "inputs.files downloadTask\n" +
            "}\n", false);
    }

    private static class Parameters {
        final String src;
        final String dest;
        final boolean overwrite;
        final boolean onlyIfNewer;
        final boolean compress;
        final boolean quiet;
        final boolean offline;

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfNewer) {
            this(src, dest, overwrite, onlyIfNewer, true, false, false);
        }

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfNewer,
                boolean compress, boolean offline, boolean quiet) {
            this.src = src;
            this.dest = dest;
            this.overwrite = overwrite;
            this.onlyIfNewer = onlyIfNewer;
            this.compress = compress;
            this.offline = offline;
            this.quiet = quiet;
        }
    }
}

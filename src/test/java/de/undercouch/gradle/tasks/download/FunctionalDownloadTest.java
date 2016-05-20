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

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

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
        assertTaskSuccess(download(singleSrc, dest, true));
        assertTrue(destFile.exists());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(destFile));
    }

    /**
     * Test if multiple files can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        assertTaskSuccess(download(multipleSrc, dest, true));
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
        assertTaskSuccess(download(singleSrc, dest, false));
        assertTaskUpToDate(download(singleSrc, dest, false));
    }

    /**
     * Download a file with 'overwrite' flag and check if the second attempt succeeds
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceWithOverwriteExecutesTwice() throws Exception {
        assertTaskSuccess(download(singleSrc, dest, false));
        assertTaskSuccess(download(singleSrc, dest, true));
    }

    /**
     * Download a file twice in offline mode and check if the second attempt is
     * skipped even if the 'overwrite' flag is set
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceWithOfflineMode() throws Exception {
        assertTaskSuccess(download(singleSrc, dest, false));
        assertTaskSkipped(downloadOffline(singleSrc, dest, true));
    }

    /**
     * Create a download task
     * @param src the src from which to download
     * @param dest the destination file
     * @param overwrite true if the overwrite flag should be set
     * @return the download task
     * @throws Exception if anything went wrong
     */
    protected BuildTask download(String src, String dest, boolean overwrite) throws Exception {
        return createRunner(src, dest, overwrite, false)
            .withArguments("download")
            .build()
            .task(":download");
    }

    /**
     * Create a download task in offline mode
     * @param src the src from which to download
     * @param dest the destination file
     * @param overwrite true if the overwrite flag should be set
     * @return the download task
     * @throws Exception if anything went wrong
     */
    protected BuildTask downloadOffline(String src, String dest, boolean overwrite) throws IOException {
        return createRunner(src, dest, overwrite, false)
            .withArguments("--offline", "download")
            .build()
            .task(":download");
    }

    /**
     * Create a gradle runner to test against
     * @param src the src from which to download
     * @param dest the destination file
     * @param overwrite true if the overwrite flag should be set
     * @param onlyIfNewer true if the onlyIfNewer flag should be set
     * @return the runner
     * @throws IOException if the build file could not be created
     */
    protected GradleRunner createRunner(String src, String dest, boolean overwrite, boolean onlyIfNewer) throws IOException {
        return createRunnerWithBuildFile(
            "plugins { id 'de.undercouch.download' }\n" +
            "task download(type: de.undercouch.gradle.tasks.download.Download) { \n" +
                "src(" + src + ")\n" +
                "dest " + dest + "\n" +
                "overwrite " + Boolean.toString(overwrite) + "\n" +
                "onlyIfNewer " + Boolean.toString(onlyIfNewer) + "\n" +
            "}\n");
    }
}

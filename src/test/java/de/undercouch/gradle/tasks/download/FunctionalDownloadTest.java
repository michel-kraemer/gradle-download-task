package de.undercouch.gradle.tasks.download;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class FunctionalDownloadTest extends FunctionalTestBase {
    private String singleSrc;
    private String multipleSrc;
    private String dest;
    private File destFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        singleSrc = "'" + makeSrc(TEST_FILE_NAME) + "'";
        multipleSrc = "['" +  makeSrc(TEST_FILE_NAME) + "', '" + makeSrc(TEST_FILE_NAME2) + "']";
        destFile = new File(testProjectDir.getRoot(), "someFile");
        dest = "file('"+destFile.getName()+"')";
    }

    @Test
    public void downloadSingleFile() throws Exception {
        assertTaskSuccess(download(singleSrc, dest, true));
        assertTrue(destFile.exists());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(destFile));
    }

    @Test
    public void downloadMultipleFiles() throws Exception {
        assertTaskSuccess(download(multipleSrc, dest, true));
        assertTrue(destFile.isDirectory());
        assertArrayEquals(contents, FileUtils.readFileToByteArray(
                new File(destFile, TEST_FILE_NAME)));
        assertArrayEquals(contents2, FileUtils.readFileToByteArray(
                new File(destFile, TEST_FILE_NAME2)));
    }

    @Test
    public void downloadSingleFileTwiceMarksTaskAsUpToDate() throws Exception {
        assertTaskSuccess(download(singleSrc, dest, false));
        assertTaskUpToDate(download(singleSrc, dest, false));
    }

    @Test
    public void downloadSingleFileTwiceWithOverwriteExecutesTwice() throws Exception {
        assertTaskSuccess(download(singleSrc, dest, false));
        assertTaskSuccess(download(singleSrc, dest, true));
    }

    @Test
    public void downloadSingleFileTwiceWithOfflineMode() throws Exception {
        assertTaskSuccess(download(singleSrc, dest, false));
        assertTaskSkipped(downloadOffline(singleSrc, dest, true));
    }

    protected BuildTask download(String src, String dest, boolean overwrite) throws Exception {
        return createRunner(src, dest, overwrite, false)
            .withArguments("download")
            .build()
            .task(":download");
    }

    protected BuildTask downloadOffline(String src, String dest, boolean overwrite) throws Exception {
        return createRunner(src, dest, overwrite, false)
            .withArguments("--offline", "download")
            .build()
            .task(":download");
    }

    protected GradleRunner createRunner(String src, String dest, boolean overwrite, boolean onlyIfNewer) throws Exception {
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

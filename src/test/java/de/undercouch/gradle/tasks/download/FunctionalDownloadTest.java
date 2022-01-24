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
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the plugin's functionality
 * @author Jan Berkel
 * @author Michel Kraemer
 */
public class FunctionalDownloadTest extends FunctionalTestBase {
    private String singleSrc;
    private String multipleSrc;
    private String dest;
    private File destFile;

    /**
     * Constructs a new functional test
     */
    @BeforeEach
    public void setUpFunctionalDownloadTest() {
        singleSrc = "'" + wireMock.url(TEST_FILE_NAME) + "'";
        multipleSrc = "['" +  wireMock.url(TEST_FILE_NAME) +
                "', '" + wireMock.url(TEST_FILE_NAME2) + "']";
        destFile = new File(testProjectDir.toFile(), "someFile");
        dest = "file('" + destFile.getName() + "')";
    }

    /**
     * Test if a single file can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully when destination is
     * a RegularFileProperty
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingRegularFileProperty() throws Exception {
        configureDefaultStub();
        String setup = "RegularFileProperty fp = project.objects.fileProperty();\n" +
                "fp.set(" + dest + ")\n";
        assertTaskSuccess(download(new Parameters(singleSrc, "fp", setup, true, false)));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully when destination
     * is a basic Property provider
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingFileProperty() throws Exception {
        configureDefaultStub();
        String setup = "Property fp = project.objects.property(File.class);\n" +
                "fp.set(" + dest + ")\n";
        assertTaskSuccess(download(new Parameters(singleSrc, "fp", setup, true, false)));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }


    /**
     * Test if a single file can be downloaded successfully when destination is
     * a file inside the buildDirectory
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingBuildDirectoryFile() throws Exception {
        configureDefaultStub();
        String dest = "layout.buildDirectory.file('download/outputfile')";
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        File destFile = new File(testProjectDir.toFile(), "build/download/outputfile");
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }


    /**
     * Test if a single file can be downloaded successfully when destination
     * is a directory inside the buildDirectory
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingBuildDirectoryDir() throws Exception {
        configureDefaultStub();
        String dest = "layout.buildDirectory.dir('download/')";
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        File[] destFiles = new File(testProjectDir.toFile(), "build/download/").listFiles();
        assertThat(destFiles).isNotNull();
        File destFile = destFiles[0];
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileWithQuietMode() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true,
                false, true, false, true)));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileWithoutCompress() throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true,
                false, false, false, false)));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if multiple files can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTaskSuccess(download(new Parameters(multipleSrc, dest, true, false)));
        assertThat(destFile).isDirectory();
        assertThat(new File(destFile, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(destFile, TEST_FILE_NAME2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Download a file twice and check if the second attempt is skipped
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceMarksTaskAsUpToDate() throws Exception {
        configureDefaultStub();
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
        configureDefaultStub();
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
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        assertTaskSkipped(download(new Parameters(singleSrc, dest, true, false,
                true, true, false)));
    }

    /**
     * Download a file once, then download again with 'onlyIfModified'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewer() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:54:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, true)));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true)));
    }

    /**
     * Download a file once, then download again with 'onlyIfModified'.
     * File changed between downloads.
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewerRedownloadsIfFileHasBeenUpdated() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:54:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, true)));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:55:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, true)));
    }

    /**
     * Download a file once, then download again with 'useETag'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadUseETag() throws Exception {
        String etag = "\"foobar\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", equalTo(etag))
                .willReturn(aResponse()
                        .withStatus(304)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, true,
                false, false, false, true)));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true,
                false, false, false, true)));
    }

    /**
     * Create destination file locally, then run download.
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewerReDownloadIfFileExists() throws Exception {
        String lm = "Sat, 21 Jun 2019 11:54:15 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        FileUtils.writeStringToFile(destFile, CONTENTS, StandardCharsets.UTF_8);
        assertThat(destFile.setLastModified(expectedlmlong)).isTrue();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
    }

    /**
     * Copy a file from a file:// URL once, then download again with 'onlyIfModified'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadFileURLOnlyIfNewer() throws Exception {
        File srcFile = newTempFile();
        FileUtils.writeStringToFile(srcFile, CONTENTS, StandardCharsets.UTF_8);
        String srcFileUri = "'" + srcFile.toURI() + "'";
        assertTaskSuccess(download(new Parameters(srcFileUri, dest, true, true)));
        assertThat(destFile.setLastModified(srcFile.lastModified())).isTrue();
        assertTaskUpToDate(download(new Parameters(srcFileUri, dest, true, true)));
    }

    /**
     * Test if the download task is triggered if another task depends on its
     * output file
     * @throws Exception if anything went wrong
     */
    @Test
    public void fileDependenciesTriggersDownloadTask() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(runTask(":processTask", new Parameters(singleSrc,
                dest, true, false)));
        assertThat(destFile).isFile();
    }

    /**
     * Test if the download task is triggered if another tasks depends on its
     * output files
     * @throws Exception if anything went wrong
     */
    @Test
    public void fileDependenciesWithMultipleSourcesTriggersDownloadTask()
            throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertThat(destFile.mkdirs()).isTrue();
        assertTaskSuccess(runTask(":processTask", new Parameters(multipleSrc,
                dest, true, false)));
        assertThat(destFile).isDirectory();
        assertThat(new File(destFile, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(destFile, TEST_FILE_NAME2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * If the download extension fails and the exception is caught, the task
     * should not automatically fail too
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadExtensionShouldNotFailTask() throws Exception{
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withStatus(404)));

        GradleRunner runner = createRunnerWithBuildFile(
                "plugins { id 'de.undercouch.download' }\n" +
                "task downloadTask {\n" +
                    "doLast {\n" +
                        "try {\n" +
                            "download.run {\n" +
                                "src " + singleSrc + "\n" +
                                "dest " + dest + "\n" +
                            "}\n" +
                        "} catch (Exception e) {\n" +
                            "println('Exception thrown: ' + e.class)\n" +
                        "}\n" +
                    "}\n" +
                "}\n");

        BuildResult result = runner.withArguments(singletonList("downloadTask"))
                .build();
        assertThat(result.getOutput()).contains("Exception thrown: class java.lang.IllegalStateException");
        assertTaskSuccess(result.task(":downloadTask"));
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
            parameters.setup +
            "task downloadTask(type: Download) {\n" +
                "src(" + parameters.src + ")\n" +
                "dest " + parameters.dest + "\n" +
                "overwrite " + parameters.overwrite + "\n" +
                "onlyIfModified " + parameters.onlyIfModified + "\n" +
                "compress " + parameters.compress + "\n" +
                "quiet " + parameters.quiet + "\n" +
                "useETag " + parameters.useETag + "\n" +
            "}\n" +
            "task processTask {\n" +
                "inputs.files files(downloadTask)\n" +
                "doLast {\n" +
                    "assert !inputs.files.isEmpty()\n" +
                    "inputs.files.each { f -> assert f.isFile() }\n" +
                "}\n" +
            "}\n");
    }

    private static class Parameters {
        final String src;
        final String dest;
        final String setup;
        final boolean overwrite;
        final boolean onlyIfModified;
        final boolean compress;
        final boolean quiet;
        final boolean offline;
        final boolean useETag;

        Parameters(String src, String dest, String setup, boolean overwrite, boolean onlyIfModified) {
            this(src, dest, setup, overwrite, onlyIfModified, true, false, false, false);
        }

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfModified) {
            this(src, dest, overwrite, onlyIfModified, true, false, false);
        }

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfModified,
                boolean compress, boolean offline, boolean quiet) {
            this(src, dest, overwrite, onlyIfModified, compress, offline, quiet, false);
        }

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfModified,
                boolean compress, boolean offline, boolean quiet, boolean useETag) {
            this(src, dest, "", overwrite, onlyIfModified, compress, offline, quiet, useETag);
        }

        Parameters(String src, String dest, String setup, boolean overwrite, boolean onlyIfModified,
                boolean compress, boolean offline, boolean quiet, boolean useETag) {
            this.src = src;
            this.dest = dest;
            this.setup = setup;
            this.overwrite = overwrite;
            this.onlyIfModified = onlyIfModified;
            this.compress = compress;
            this.offline = offline;
            this.quiet = quiet;
            this.useETag = useETag;
        }
    }
}

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * @return the Gradle versions to test against
     */
    public static Stream<Arguments> versionsToTest() {
        List<String> versions;
        if ("true".equals(System.getenv("CI"))) {
            // on CI server, limit to major versions to avoid running
            // out of open file descriptors (happens when we load the
            // jar files of too many Gradle distributions into memory)
            versions = Arrays.asList(
                    "5.6.4",
                    "6.9.2",
                    "7.3.3"
            );
        } else {
            versions = Arrays.asList(
                    "5.0", "5.1", "5.1.1", "5.2", "5.2.1", "5.3", "5.3.1",
                    "5.4", "5.4.1", "5.5", "5.5.1",
                    "5.6", "5.6.1", "5.6.2", "5.6.3", "5.6.4",
                    "6.0", "6.0.1", "6.1", "6.1.1", "6.2", "6.2.1", "6.2.2",
                    "6.3", "6.4", "6.4.1", "6.5", "6.5.1", "6.6", "6.6.1",
                    "6.7", "6.7.1", "6.8", "6.8.1", "6.8.2", "6.8.3",
                    "6.9", "6.9.1", "6.9.2",
                    "7.0", "7.0.1", "7.0.2", "7.1", "7.1.1", "7.2",
                    "7.3", "7.3.1", "7.3.2", "7.3.3"
            );
        }

        // get methods annotated with @FunctionalTest
        List<String> methods = Arrays.stream(FunctionalDownloadTest.class.getDeclaredMethods())
                .filter(m -> Arrays.stream(m.getDeclaredAnnotations())
                                .anyMatch(a -> a.annotationType() == FunctionalTest.class))
                .map(Method::getName)
                .collect(Collectors.toList());

        // Run each @FunctionalTest for each version
        // Tests should be sorted by version, so the Gradle daemon can be
        // reused. Otherwise, the tests will be really slow.
        return versions.stream().flatMap(a -> methods.stream().flatMap(b ->
                Stream.of(Arguments.of(a, b))));
    }

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
     * We use our own strategy to call test methods based on the parameters
     * provided by {@link #versionsToTest()} instead of parameterized tests
     * from JUnit 5. Parameterized tests apply to a single test and not to the
     * whole class. This means JUnit calls one test method n times for n
     * parameters and then continues with the next method. In our case, this
     * means that the tests are very slow, because Gradle daemons cannot be
     * reused. Instead, we have to call each test method for one version and
     * then continue with the next version (i.e. we have to group test methods
     * by version). This allows Gradle daemons to be reused.
     */
    @ParameterizedTest()
    @MethodSource("versionsToTest")
    public void test(String gradleVersion, String testMethod) throws Exception {
        Method m = FunctionalDownloadTest.class.getDeclaredMethod(testMethod, String.class);
        m.invoke(this, gradleVersion);
    }

    /**
     * Test if a single file can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFile(String gradleVersion) throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false),
                gradleVersion));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully when destination is
     * a RegularFileProperty
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileUsingRegularFileProperty(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        String setup = "RegularFileProperty fp = project.objects.fileProperty();\n" +
                "fp.set(" + dest + ")\n";
        assertTaskSuccess(download(new Parameters(singleSrc, "fp", setup, true, false),
                gradleVersion));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully when destination
     * is a basic Property provider
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileUsingFileProperty(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        String setup = "Property fp = project.objects.property(File.class);\n" +
                "fp.set(" + dest + ")\n";
        assertTaskSuccess(download(new Parameters(singleSrc, "fp", setup, true, false),
                gradleVersion));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }


    /**
     * Test if a single file can be downloaded successfully when destination is
     * a file inside the buildDirectory
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileUsingBuildDirectoryFile(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        String dest = "layout.buildDirectory.file('download/outputfile')";
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false),
                gradleVersion));
        File destFile = new File(testProjectDir.toFile(), "build/download/outputfile");
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }


    /**
     * Test if a single file can be downloaded successfully when destination
     * is a directory inside the buildDirectory
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileUsingBuildDirectoryDir(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        String dest = "layout.buildDirectory.dir('download/')";
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false),
                gradleVersion));
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
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileWithQuietMode(String gradleVersion) throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true,
                false, true, false, true), gradleVersion));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileWithoutCompress(String gradleVersion) throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true,
                false, false, false, false), gradleVersion));
        assertThat(destFile).isFile();
        assertThat(destFile).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if multiple files can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadMultipleFiles(String gradleVersion) throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTaskSuccess(download(new Parameters(multipleSrc, dest, true, false),
                gradleVersion));
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
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileTwiceMarksTaskAsUpToDate(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        final Parameters parameters = new Parameters(singleSrc, dest, false, false);
        assertTaskSuccess(download(parameters, gradleVersion));
        assertTaskUpToDate(download(parameters, gradleVersion));
    }

    /**
     * Download a file with 'overwrite' flag and check if the second attempt succeeds
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileTwiceWithOverwriteExecutesTwice(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false),
                gradleVersion));
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false),
                gradleVersion));
    }

    /**
     * Download a file twice in offline mode and check if the second attempt is
     * skipped even if the 'overwrite' flag is set
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadSingleFileTwiceWithOfflineMode(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false),
                gradleVersion));
        assertTaskSkipped(download(new Parameters(singleSrc, dest, true, false,
                true, true, false), gradleVersion));
    }

    /**
     * Download a file once, then download again with 'onlyIfModified'
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadOnlyIfNewer(String gradleVersion) throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:54:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, true),
                gradleVersion));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true),
                gradleVersion));
    }

    /**
     * Download a file once, then download again with 'onlyIfModified'.
     * File changed between downloads.
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadOnlyIfNewerRedownloadsIfFileHasBeenUpdated(String gradleVersion)
            throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:54:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, true),
                gradleVersion));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:55:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, true),
                gradleVersion));
    }

    /**
     * Download a file once, then download again with 'useETag'
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadUseETag(String gradleVersion) throws Exception {
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
                false, false, false, true), gradleVersion));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true,
                false, false, false, true), gradleVersion));
    }

    /**
     * Create destination file locally, then run download.
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadOnlyIfNewerReDownloadIfFileExists(String gradleVersion)
            throws Exception {
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
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false),
                gradleVersion));
    }

    /**
     * Copy a file from a file:// URL once, then download again with 'onlyIfModified'
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void downloadFileURLOnlyIfNewer(String gradleVersion) throws Exception {
        File srcFile = newTempFile();
        FileUtils.writeStringToFile(srcFile, CONTENTS, StandardCharsets.UTF_8);
        String srcFileUri = "'" + srcFile.toURI() + "'";
        assertTaskSuccess(download(new Parameters(srcFileUri, dest, true, true),
                gradleVersion));
        assertThat(destFile.setLastModified(srcFile.lastModified())).isTrue();
        assertTaskUpToDate(download(new Parameters(srcFileUri, dest, true, true),
                gradleVersion));
    }

    /**
     * Test if the download task is triggered if another task depends on its
     * output file
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void fileDependenciesTriggersDownloadTask(String gradleVersion)
            throws Exception {
        configureDefaultStub();
        assertTaskSuccess(runTask(":processTask", new Parameters(singleSrc,
                dest, true, false), gradleVersion));
        assertThat(destFile).isFile();
    }

    /**
     * Test if the download task is triggered if another tasks depends on its
     * output files
     * @throws Exception if anything went wrong
     */
    @FunctionalTest
    @SuppressWarnings("unused")
    private void fileDependenciesWithMultipleSourcesTriggersDownloadTask(
            String gradleVersion) throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertThat(destFile.mkdirs()).isTrue();
        assertTaskSuccess(runTask(":processTask", new Parameters(multipleSrc,
                dest, true, false), gradleVersion));
        assertThat(destFile).isDirectory();
        assertThat(new File(destFile, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(destFile, TEST_FILE_NAME2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Create a download task
     * @param parameters the download parameters
     * @param gradleVersion the Gradle version to test against
     * @return the download task
     * @throws Exception if anything went wrong
     */
    protected BuildTask download(Parameters parameters,
            String gradleVersion) throws Exception {
        return runTask(":downloadTask", parameters, gradleVersion);
    }

    /**
     * Create a task
     * @param taskName the task's name
     * @param parameters the download parameters
     * @param gradleVersion the Gradle version to test against
     * @return the task
     * @throws Exception if anything went wrong
     */
    protected BuildTask runTask(String taskName, Parameters parameters,
            String gradleVersion) throws Exception {
        return createRunner(parameters, gradleVersion)
                .withArguments(parameters.offline ? asList("--offline", taskName) :
                    singletonList(taskName))
                .build()
                .task(taskName);
    }

    /**
     * Create a gradle runner to test against
     * @param parameters the download parameters
     * @param gradleVersion the Gradle version to test against
     * @return the runner
     * @throws IOException if the build file could not be created
     */
    protected GradleRunner createRunner(Parameters parameters,
            String gradleVersion) throws IOException {
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
            "}\n", gradleVersion);
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

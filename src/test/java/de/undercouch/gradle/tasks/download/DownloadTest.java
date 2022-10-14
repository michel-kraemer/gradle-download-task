package de.undercouch.gradle.tasks.download;

import groovy.lang.Closure;
import kotlin.jvm.functions.Function0;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests the gradle-download-task plugin
 * @author Michel Kraemer
 */
public class DownloadTest extends TestBaseWithMockServer {
    /**
     * Create a WireMock stub for {@link #TEST_FILE_NAME} with {@link #CONTENTS}
     * and {@link #TEST_FILE_NAME2} with {@link #CONTENTS2}
     */
    @BeforeEach
    public void stubForTestFile() {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("content-length", String.valueOf(CONTENTS.length()))
                        .withBody(CONTENTS)));
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME2))
                .willReturn(aResponse()
                        .withHeader("content-length", String.valueOf(CONTENTS2.length()))
                        .withBody(CONTENTS2)));
    }

    /**
     * Tests if a single file can be downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        Download t = makeProjectAndTask();
        String src = wireMock.url(TEST_FILE_NAME);
        t.src(src);
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(t.getSrc()).isInstanceOf(URL.class);
        assertThat(t.getSrc().toString()).isEqualTo(src);
        assertThat(t.getDest()).isSameAs(dst);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if a single file can be downloaded from a URL
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleURL() throws Exception {
        Download t = makeProjectAndTask();
        URL src = new URL(wireMock.url(TEST_FILE_NAME));
        t.src(src);
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(t.getSrc()).isSameAs(src);
        assertThat(t.getDest()).isSameAs(dst);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if a single file can be downloaded to a directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFileToDir() throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempDir();
        t.dest(dst);
        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
    }

    /**
     * Tests if a file is downloaded to the project directory when specifying
     * a relative path
     */
    @Test
    public void downloadSingleFileToRelativePath() {
        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        t.dest(TEST_FILE_NAME);
        execute(t);

        assertThat(new File(projectDir, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
    }

    /**
     * Tests if multiple files can be downloaded to a directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMock.url(TEST_FILE_NAME),
                wireMock.url(TEST_FILE_NAME2)));

        File dst = newTempDir();
        t.dest(dst);
        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, TEST_FILE_NAME2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Tests if a destination directory is automatically created if multiple
     * files are downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFilesCreatesDestDirAutomatically() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMock.url(TEST_FILE_NAME),
                wireMock.url(TEST_FILE_NAME2)));

        File dst = newTempDir();
        assertThat(dst.delete()).isTrue();
        t.dest(dst);
        execute(t);
        assertThat(dst).isDirectory();
    }

    /**
     * Tests if the task throws an exception if you try to download
     * multiple files to a single destination file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFilesToFile() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMock.url(TEST_FILE_NAME),
                wireMock.url(TEST_FILE_NAME2)));
        File dst = newTempFile();
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the destination has to be a directory");
    }

    /**
     * Tests lazy evaluation of 'src' and 'dest' properties
     * @throws Exception if anything goes wrong
     */
    @Test
    public void lazySrcAndDest() throws Exception {
        final boolean[] srcCalled = new boolean[] { false };
        final boolean[] dstCalled = new boolean[] { false };

        final File dst = newTempFile();

        Download t = makeProjectAndTask();
        t.src(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -4463658999363261400L;

            @SuppressWarnings("unused")
            public Object doCall() {
                srcCalled[0] = true;
                return wireMock.url(TEST_FILE_NAME);
            }
        });

        t.dest(new Closure<Object>(this, this) {
            private static final long serialVersionUID = 932174549047352157L;

            @SuppressWarnings("unused")
            public Object doCall() {
                dstCalled[0] = true;
                return dst;
            }
        });

        assertThat(srcCalled[0]).isFalse();
        assertThat(dstCalled[0]).isFalse();

        execute(t);

        assertThat(srcCalled[0]).isTrue();
        assertThat(dstCalled[0]).isTrue();

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests lazy evaluation of 'src' and 'dest' properties if they are
     * Providers
     * @throws Exception if anything goes wrong
     */
    @Test
    public void providerSrcAndDest() throws Exception {
        final boolean[] srcCalled = new boolean[] { false };
        final boolean[] dstCalled = new boolean[] { false };

        final File dst = newTempFile();

        Download t = makeProjectAndTask();
        t.src(new DefaultProvider<>((Callable<Object>)() -> {
            srcCalled[0] = true;
            return wireMock.url(TEST_FILE_NAME);
        }));

        t.dest(new DefaultProvider<>((Callable<Object>)() -> {
            dstCalled[0] = true;
            return dst;
        }));

        assertThat(srcCalled[0]).isFalse();
        assertThat(dstCalled[0]).isFalse();

        execute(t);

        assertThat(srcCalled[0]).isTrue();
        assertThat(dstCalled[0]).isTrue();

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests lazy evaluation of 'src' and 'dest' properties if they are
     * Kotlin Functions
     * @throws Exception if anything goes wrong
     */
    @Test
    public void kotlinFunctionSrcAndDest() throws Exception {
        final boolean[] srcCalled = new boolean[] { false };
        final boolean[] dstCalled = new boolean[] { false };

        final File dst = newTempFile();

        Download t = makeProjectAndTask();
        t.src((Function0<Object>)() -> {
            srcCalled[0] = true;
            return wireMock.url(TEST_FILE_NAME);
        });

        t.dest((Function0<Object>)() -> {
            dstCalled[0] = true;
            return dst;
        });

        assertThat(srcCalled[0]).isFalse();
        assertThat(dstCalled[0]).isFalse();

        execute(t);

        assertThat(srcCalled[0]).isTrue();
        assertThat(dstCalled[0]).isTrue();

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Test if cached sources are updated properly
     * @throws Exception if anything goes wrong
     */
    @Test
    public void updateCachedSources() throws Exception {
        final AtomicInteger srcCalled = new AtomicInteger();

        final File dst = newTempDir();

        Download t = makeProjectAndTask();
        t.src(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -4463658999363261400L;

            @SuppressWarnings("unused")
            public Object doCall() {
                srcCalled.incrementAndGet();
                return wireMock.url(TEST_FILE_NAME);
            }
        });

        assertThat(srcCalled.get()).isEqualTo(0);

        // calling t.getSrc() will call our closure and cache the result
        assertThat(t.getSrc()).isInstanceOf(URL.class);
        assertThat(srcCalled.get()).isEqualTo(1);

        // add another src
        t.src(wireMock.url(TEST_FILE_NAME2));

        // getSrc() should return the updated sources but our closure should
        // still have only been called once
        assertThat(t.getSrc()).isInstanceOf(List.class);
        assertThat(srcCalled.get()).isEqualTo(1);

        t.dest(dst);

        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, TEST_FILE_NAME2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Make sure eachFile actions are called only once
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileCalledOnce() throws Exception {
        final AtomicInteger eachFileCalled = new AtomicInteger();

        final File dst = newTempDir();
        File destFile1 = new File(dst, TEST_FILE_NAME);
        File destFile2 = new File(dst, TEST_FILE_NAME2);

        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2));
        t.dest(dst);
        t.eachFile(f -> eachFileCalled.incrementAndGet());

        assertThat(eachFileCalled.get()).isEqualTo(0);

        // calling t.getOutputFiles() will call the eachFile action (once for
        // each source) and cache the result
        assertThat(t.getOutputFiles()).isEqualTo(Arrays.asList(destFile1, destFile2));
        assertThat(eachFileCalled.get()).isEqualTo(2);

        // call it again, the eachFile action should not be called again
        assertThat(t.getOutputFiles()).isEqualTo(Arrays.asList(destFile1, destFile2));
        assertThat(eachFileCalled.get()).isEqualTo(2);

        execute(t);

        assertThat(destFile1)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(destFile2)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);

        // eachFile action should not have been called again
        assertThat(eachFileCalled.get()).isEqualTo(2);
    }

    /**
     * Test if the cached list of output files is updated properly
     * @throws Exception if anything goes wrong
     */
    @Test
    public void updateCachedOutputFiles() throws Exception {
        final AtomicInteger eachFileCalled = new AtomicInteger();

        final File dst = newTempDir();
        File destFile1 = new File(dst, TEST_FILE_NAME + ".1");
        File destFile2 = new File(dst, TEST_FILE_NAME2 + ".2");
        File destFile3 = new File(dst, TEST_FILE_NAME2 + ".3");

        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2));
        t.dest(dst);
        t.eachFile(f -> f.setName(f.getName() + "." + eachFileCalled.incrementAndGet()));

        assertThat(eachFileCalled.get()).isEqualTo(0);

        // t.getOutputFiles() will call the eachFile action (once for each
        // source) and cache the result
        assertThat(t.getOutputFiles()).isEqualTo(Arrays.asList(destFile1, destFile2));
        assertThat(eachFileCalled.get()).isEqualTo(2);

        // add another src
        t.src(wireMock.url(TEST_FILE_NAME2));

        // t.getOutputFiles() will now call the eachFile action again for the
        // new source and then cache the new result
        assertThat(t.getOutputFiles()).isEqualTo(Arrays.asList(destFile1, destFile2, destFile3));
        assertThat(eachFileCalled.get()).isEqualTo(3);

        t.dest(dst);

        execute(t);

        assertThat(destFile1)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(destFile2)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
        assertThat(destFile3)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);

        // the eachFile action should still have been called only three times
        assertThat(eachFileCalled.get()).isEqualTo(3);
    }

    /**
     * Do not overwrite an existing file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void skipExisting() throws Exception {
        // write contents to destination file
        File dst = newTempFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);

        Download t = makeProjectAndTask();
        String src = wireMock.url(TEST_FILE_NAME);
        t.src(src);
        t.dest(dst);
        t.overwrite(false); // do not overwrite the file
        execute(t);

        // contents must not be changed
        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent("Hello");
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is invalid
     * @throws Exception if the test succeeds
     */
    @Test
    public void testInvalidSrc() throws Exception {
        Download t = makeProjectAndTask();
        t.src(new Object());
        t.dest(newTempFile());
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Download source must either be");
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is empty
     */
    @Test
    public void testExecuteEmptySrc() {
        Download t = makeProjectAndTask();
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please provide a download source");
    }

    /**
     * Test if the plugin throws an exception if the 'dest' property is invalid
     */
    @Test
    public void testInvalidDest() {
        Download t = makeProjectAndTask();
        String src = wireMock.url(TEST_FILE_NAME);
        t.src(src);
        t.dest(new Object());
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Download destination must be one of");
    }

    /**
     * Test if the plugin throws an exception if the 'dest' property is empty
     */
    @Test
    public void testExecuteEmptyDest() {
        Download t = makeProjectAndTask();
        String src = wireMock.url(TEST_FILE_NAME);
        t.src(src);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please provide a download destination");
    }

    /**
     * Test if the plugin can handle an array containing one string as source
     */
    @Test
    public void testArraySrc() {
        Download t = makeProjectAndTask();
        String src = wireMock.url(TEST_FILE_NAME);
        t.src(new Object[] { src });
        assertThat(t.getSrc()).isInstanceOf(URL.class);
    }

    /**
     * Test if a file can be "downloaded" from a file:// url
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testFileDownloadURL() throws Exception {
        Download t = makeProjectAndTask();

        String testContent = "file content";
        File src = newTempFile();
        FileUtils.writeStringToFile(src, testContent, "UTF-8");

        URL url = src.toURI().toURL();

        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();

        t.src(new Object[] { url.toExternalForm() });
        t.dest(dst);
        execute(t);

        String content = FileUtils.readFileToString(dst, "UTF-8");
        assertThat(content).isEqualTo(testContent);
    }

    /**
     * Test if a file can be "downloaded" from a file:// url with the
     * overwrite flag set to true
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testFileDownloadURLOverwriteTrue() throws Exception {
        Download t = makeProjectAndTask();

        String testContent = "file content";
        File src = newTempFile();
        FileUtils.writeStringToFile(src, testContent, "UTF-8");

        URL url = src.toURI().toURL();

        File dst = newTempFile();
        assertThat(dst).exists();

        t.src(new Object[] { url.toExternalForm() });
        t.dest(dst);
        t.overwrite(true);
        execute(t);

        String content = FileUtils.readFileToString(dst, "UTF-8");
        assertThat(content).isEqualTo(testContent);
    }

    /**
     * Tests if specifying an eachFile action leads to an exception if only
     * one source is given
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionSingleSource() throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        t.dest(newTempFile());
        t.eachFile(details -> {
            fail("We should never get here");
        });
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eachFile");
    }

    /**
     * Tests if we can catch an exception from an eachFile action
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionThrows() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMock.url(TEST_FILE_NAME), wireMock.url(TEST_FILE_NAME2)));
        t.dest(newTempDir());
        t.eachFile(details -> {
            throw new RuntimeException("Dummy");
        });
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Dummy");
    }

    /**
     * Tests if multiple files can be downloaded and an {@code eachFile} action
     * is called for each of them
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionMultipleFiles() throws Exception {
        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2));

        File dst = newTempDir();
        t.dest(dst);

        AtomicInteger calls = new AtomicInteger(0);
        t.eachFile(details -> {
            if (details.getSourceURL().toString().equals(u1)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME);
                calls.incrementAndGet();
            } else if (details.getSourceURL().toString().equals(u2)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME2);
                calls.incrementAndGet();
            } else {
                fail("Unknown source URL: " + details.getSourceURL());
            }
        });
        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, TEST_FILE_NAME2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
        assertThat(calls.get()).isEqualTo(2);
    }

    /**
     * Tests if multiple files can be downloaded and an {@code eachFile} action
     * can be applied to rename them
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionMultipleFilesRename() throws Exception {
        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2));

        String nn1 = TEST_FILE_NAME + ".renamed";
        String nn2 = TEST_FILE_NAME2 + ".renamed";

        File dst = newTempDir();
        t.dest(dst);

        t.eachFile(details -> {
            if (details.getSourceURL().toString().equals(u1)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME);
                details.setName(nn1);
            } else if (details.getSourceURL().toString().equals(u2)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME2);
                details.setName(nn2);
            } else {
                fail("Unknown source URL: " + details.getSourceURL());
            }
        });
        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME)).doesNotExist();
        assertThat(new File(dst, TEST_FILE_NAME2)).doesNotExist();
        assertThat(new File(dst, nn1))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, nn2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Tests if multiple files can be downloaded and an {@code eachFile} action
     * can be applied to change their relative path
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionMultipleFilesRelativePath() throws Exception {
        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2));

        String nn1 = "foo/" + TEST_FILE_NAME;
        String nn2 = "foo/" + TEST_FILE_NAME2;

        File dst = newTempDir();
        t.dest(dst);

        t.eachFile(details -> {
            if (details.getSourceURL().toString().equals(u1)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME);
                assertThat(details.getPath()).isEqualTo(TEST_FILE_NAME);
                assertThat((Object)details.getRelativePath())
                        .isEqualTo(RelativePath.parse(true, TEST_FILE_NAME));
                // use setRelativePath()
                details.setRelativePath(RelativePath.parse(true, nn1));
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME);
                assertThat(details.getPath()).isEqualTo(nn1);
                assertThat((Object)details.getRelativePath())
                        .isEqualTo(RelativePath.parse(true, nn1));
            } else if (details.getSourceURL().toString().equals(u2)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME2);
                assertThat(details.getPath()).isEqualTo(TEST_FILE_NAME2);
                assertThat((Object)details.getRelativePath())
                        .isEqualTo(RelativePath.parse(true, TEST_FILE_NAME2));
                // use setPath()
                details.setPath(nn2);
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME2);
                assertThat(details.getPath()).isEqualTo(nn2);
                assertThat((Object)details.getRelativePath())
                        .isEqualTo(RelativePath.parse(true, nn2));
            } else {
                fail("Unknown source URL: " + details.getSourceURL());
            }
        });
        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME)).doesNotExist();
        assertThat(new File(dst, TEST_FILE_NAME2)).doesNotExist();
        assertThat(new File(dst, nn1))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, nn2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Tests if multiple files can be downloaded and an {@code eachFile} action
     * can be applied to change their relative path by setting their name
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionMultipleFilesRelativePathByName() throws Exception {
        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2));

        String nn1 = "foo/" + TEST_FILE_NAME;
        String nn2 = "foo/" + TEST_FILE_NAME2;

        File dst = newTempDir();
        t.dest(dst);

        t.eachFile(details -> {
            if (details.getSourceURL().toString().equals(u1)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME);
                assertThat(details.getPath()).isEqualTo(TEST_FILE_NAME);
                assertThat((Object)details.getRelativePath())
                        .isEqualTo(RelativePath.parse(true, TEST_FILE_NAME));
                // use setName() instead of setRelativePath() or setPath()
                details.setName(nn1);
                assertThat(details.getName()).isEqualTo(nn1);
                assertThat(details.getPath()).isEqualTo(nn1);
                assertThat(details.getRelativePath().getPathString())
                        .isEqualTo(RelativePath.parse(true, nn1).getPathString());
            } else if (details.getSourceURL().toString().equals(u2)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME2);
                assertThat(details.getPath()).isEqualTo(TEST_FILE_NAME2);
                assertThat((Object)details.getRelativePath())
                        .isEqualTo(RelativePath.parse(true, TEST_FILE_NAME2));
                // use setName() instead of setRelativePath() or setPath()
                details.setName(nn2);
                assertThat(details.getName()).isEqualTo(nn2);
                assertThat(details.getPath()).isEqualTo(nn2);
                assertThat(details.getRelativePath().getPathString())
                        .isEqualTo(RelativePath.parse(true, nn2).getPathString());
            } else {
                fail("Unknown source URL: " + details.getSourceURL());
            }
        });
        execute(t);

        assertThat(new File(dst, TEST_FILE_NAME)).doesNotExist();
        assertThat(new File(dst, TEST_FILE_NAME2)).doesNotExist();
        assertThat(new File(dst, nn1))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, nn2))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }

    /**
     * Tests if multiple files can be downloaded and duplicate URLs can be
     * renamed by an {@code eachFile} action
     * @throws Exception if anything goes wrong
     */
    @Test
    public void eachFileActionMultipleFilesDuplicate() throws Exception {
        Download t = makeProjectAndTask();
        String u1 = wireMock.url(TEST_FILE_NAME);
        String u2 = wireMock.url(TEST_FILE_NAME2);
        String u3 = wireMock.url(TEST_FILE_NAME2);
        t.src(Arrays.asList(u1, u2, u3));

        File dst = newTempDir();
        t.dest(dst);

        AtomicInteger calls1 = new AtomicInteger(0);
        AtomicInteger calls2 = new AtomicInteger(0);
        t.eachFile(details -> {
            if (details.getSourceURL().toString().equals(u1)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME);
                calls1.incrementAndGet();
                details.setName("1.txt");
            } else if (details.getSourceURL().toString().equals(u2)) {
                assertThat(details.getName()).isEqualTo(TEST_FILE_NAME2);
                if (calls2.incrementAndGet() == 1) {
                    details.setName("2.txt");
                } else {
                    details.setName("3.txt");
                }
            } else {
                fail("Unknown source URL: " + details.getSourceURL());
            }
        });
        execute(t);

        assertThat(calls1.get()).isEqualTo(1);
        assertThat(calls2.get()).isEqualTo(2);
        assertThat(new File(dst, TEST_FILE_NAME)).doesNotExist();
        assertThat(new File(dst, TEST_FILE_NAME2)).doesNotExist();
        assertThat(new File(dst, "1.txt"))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS);
        assertThat(new File(dst, "2.txt"))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
        assertThat(new File(dst, "3.txt"))
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(CONTENTS2);
    }
}

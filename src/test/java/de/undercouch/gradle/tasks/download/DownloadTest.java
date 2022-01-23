package de.undercouch.gradle.tasks.download;

import groovy.lang.Closure;
import kotlin.jvm.functions.Function0;
import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.provider.DefaultProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}

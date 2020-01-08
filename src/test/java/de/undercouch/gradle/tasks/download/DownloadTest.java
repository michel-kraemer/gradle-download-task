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

import groovy.lang.Closure;
import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests the gradle-download-task plugin
 * @author Michel Kraemer
 */
public class DownloadTest extends TestBaseWithMockServer {
    /**
     * Create a WireMock stub for {@link #TEST_FILE_NAME} with {@link #CONTENTS}
     * and {@link #TEST_FILE_NAME2} with {@link #CONTENTS2}
     */
    @Before
    public void stubForTestFile() {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("content-length", String.valueOf(CONTENTS.length()))
                        .withBody(CONTENTS)));
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME2))
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
        String src = wireMockRule.url(TEST_FILE_NAME);
        t.src(src);
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
        
        assertTrue(t.getSrc() instanceof URL);
        assertEquals(src, t.getSrc().toString());
        assertSame(dst, t.getDest());
        
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Tests if a single file can be downloaded from a URL
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleURL() throws Exception {
        Download t = makeProjectAndTask();
        URL src = new URL(wireMockRule.url(TEST_FILE_NAME));
        t.src(src);
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        assertSame(src, t.getSrc());
        assertSame(dst, t.getDest());

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Tests if a single file can be downloaded to a directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFileToDir() throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFolder();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(
                new File(dst, TEST_FILE_NAME));
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Tests if a file is downloaded to the project directory when specifying
     * a relative path
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFileToRelativePath() throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        t.dest(TEST_FILE_NAME);
        t.execute();

        String dstContents = FileUtils.readFileToString(
                new File(projectDir, TEST_FILE_NAME));
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Tests if multiple files can be downloaded to a directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMockRule.url(TEST_FILE_NAME),
                wireMockRule.url(TEST_FILE_NAME2)));

        File dst = folder.newFolder();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(
                new File(dst, TEST_FILE_NAME));
        assertEquals(CONTENTS, dstContents);
        String dstContents2 = FileUtils.readFileToString(
                new File(dst, TEST_FILE_NAME2));
        assertEquals(CONTENTS2, dstContents2);
    }

    /**
     * Tests if a destination directory is automatically created if multiple
     * files are downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFilesCreatesDestDirAutomatically() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMockRule.url(TEST_FILE_NAME),
                wireMockRule.url(TEST_FILE_NAME2)));

        File dst = folder.newFolder();
        assertTrue(dst.delete());
        t.dest(dst);
        t.execute();
        assertTrue(dst.isDirectory());
    }
    
    /**
     * Tests if the task throws an exception if you try to download
     * multiple files to a single destination file
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void downloadMultipleFilesToFile() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMockRule.url(TEST_FILE_NAME),
                wireMockRule.url(TEST_FILE_NAME2)));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
    }

    /**
     * Tests lazy evaluation of 'src' and 'dest' properties
     * @throws Exception if anything goes wrong
     */
    @Test
    public void lazySrcAndDest() throws Exception {
        final boolean[] srcCalled = new boolean[] { false };
        final boolean[] dstCalled = new boolean[] { false };

        final File dst = folder.newFile();

        Download t = makeProjectAndTask();
        t.src(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -4463658999363261400L;

            @SuppressWarnings("unused")
            public Object doCall() {
                srcCalled[0] = true;
                return wireMockRule.url(TEST_FILE_NAME);
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

        assertFalse(srcCalled[0]);
        assertFalse(dstCalled[0]);

        t.execute();

        assertTrue(srcCalled[0]);
        assertTrue(dstCalled[0]);

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(CONTENTS, dstContents);
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

        final File dst = folder.newFile();

        Download t = makeProjectAndTask();
        t.src(new DefaultProvider<>(new Callable<Object>() {
            public Object call() {
                srcCalled[0] = true;
                return wireMockRule.url(TEST_FILE_NAME);
            }
        }));

        t.dest(new DefaultProvider<>(new Callable<Object>() {
            public Object call() {
                dstCalled[0] = true;
                return dst;
            }
        }));

        assertFalse(srcCalled[0]);
        assertFalse(dstCalled[0]);

        t.execute();

        assertTrue(srcCalled[0]);
        assertTrue(dstCalled[0]);

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Do not overwrite an existing file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void skipExisting() throws Exception {
        // write contents to destination file
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");

        Download t = makeProjectAndTask();
        String src = wireMockRule.url(TEST_FILE_NAME);
        t.src(src);
        t.dest(dst);
        t.overwrite(false); // do not overwrite the file
        t.execute();

        // contents must not be changed
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Hello", dstContents);
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is invalid
     * @throws Exception if the test succeeds
     */
    @Test(expected = TaskExecutionException.class)
    public void testInvalidSrc() throws Exception {
        Download t = makeProjectAndTask();
        t.src(new Object());
        t.dest(folder.newFile());
        t.execute();
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is empty
     */
    @Test(expected = TaskExecutionException.class)
    public void testExecuteEmptySrc() {
        Download t = makeProjectAndTask();
        t.execute();
    }

    /**
     * Test if the plugin throws an exception if the 'dest' property is invalid
     */
    @Test(expected = TaskExecutionException.class)
    public void testInvalidDest() {
        Download t = makeProjectAndTask();
        String src = wireMockRule.url(TEST_FILE_NAME);
        t.src(src);
        t.dest(new Object());
        t.execute();
    }

    /**
     * Test if the plugin throws an exception if the 'dest' property is empty
     */
    @Test(expected = TaskExecutionException.class)
    public void testExecuteEmptyDest() {
        Download t = makeProjectAndTask();
        String src = wireMockRule.url(TEST_FILE_NAME);
        t.src(src);
        t.execute();
    }

    /**
     * Test if the plugin can handle an array containing one string as source
     */
    @Test
    public void testArraySrc() {
        Download t = makeProjectAndTask();
        String src = wireMockRule.url(TEST_FILE_NAME);
        t.src(new Object[] { src });
        assertTrue(t.getSrc() instanceof URL);
    }

    /**
     * Test if a file can be "downloaded" from a file:// url
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testFileDownloadURL() throws Exception {
        Download t = makeProjectAndTask();

        String testContent = "file content";
        File src = folder.newFile();
        FileUtils.writeStringToFile(src, testContent, "UTF-8");

        URL url = src.toURI().toURL();

        File dst = folder.newFile();
        assertTrue(dst.delete());

        t.src(new Object[] { url.toExternalForm() });
        t.dest(dst);
        t.execute();

        String content = FileUtils.readFileToString(dst, "UTF-8");
        assertEquals(testContent, content);
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
        File src = folder.newFile();
        FileUtils.writeStringToFile(src, testContent, "UTF-8");

        URL url = src.toURI().toURL();

        File dst = folder.newFile();
        assertTrue(dst.exists());

        t.src(new Object[] { url.toExternalForm() });
        t.dest(dst);
        t.overwrite(true);
        t.execute();

        String content = FileUtils.readFileToString(dst, "UTF-8");
        assertEquals(testContent, content);
    }
}

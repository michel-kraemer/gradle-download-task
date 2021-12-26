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

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests if the plugin uses the ETag header correctly
 * @author Michel Kraemer
 */
public class ETagTest extends TestBaseWithMockServer {
    /**
     * Tests if the plugin can handle a missing ETag header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingETag() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
        assertFalse(t.getCachedETagsFile().exists());
    }
    
    /**
     * Tests if the plugin can handle an incorrect (unquoted) ETag header and
     * still downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectETag() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", "abcd")
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Tests if the plugin downloads two files and stores their etags correctly
     * to the default cached etags file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeMultipleETags() throws Exception {
        String etag1 = "\"foobar1\"";
        String etag2 = "\"foobar2\"";

        wireMockRule.stubFor(get(urlEqualTo("/file1"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag1)
                        .withBody(CONTENTS + "1")));
        wireMockRule.stubFor(get(urlEqualTo("/file2"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag2)
                        .withBody(CONTENTS + "2")));

        // download first file
        Download t = makeProjectAndTask();
        t.src(wireMockRule.url("file1"));
        File dst1 = folder.newFile();
        assertTrue(dst1.delete());
        assertFalse(dst1.exists());
        t.dest(dst1);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        t.execute();

        // download second file
        t = makeProjectAndTask();
        t.src(wireMockRule.url("file2"));
        File dst2 = folder.newFile();
        assertTrue(dst2.delete());
        assertFalse(dst2.exists());
        t.dest(dst2);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        t.execute();

        // check server responses
        String dst1Contents = FileUtils.readFileToString(dst1,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS + "1", dst1Contents);
        String dst2Contents = FileUtils.readFileToString(dst2,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS + "2", dst2Contents);

        // read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        // check cached etags
        Map<String, Object> expectedETag1 = new LinkedHashMap<>();
        expectedETag1.put("ETag", etag1);
        Map<String, Object> expectedETag2 = new LinkedHashMap<>();
        expectedETag2.put("ETag", etag2);

        Map<String, Object> expectedHost = new LinkedHashMap<>();
        expectedHost.put("/file1", expectedETag1);
        expectedHost.put("/file2", expectedETag2);

        Map<String, Object> expectedCachedETags = new LinkedHashMap<>();
        expectedCachedETags.put(wireMockRule.baseUrl(), expectedHost);

        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the plugin downloads a file and stores the etag correctly to
     * the default cached etags file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeETag() throws Exception {
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        assertTrue((Boolean)t.getUseETag());
        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);

        File buildDir = new File(projectDir, "build");
        File downloadTaskDir = new File(buildDir, "download-task");
        assertEquals(downloadTaskDir.getCanonicalFile(), t.getDownloadTaskDir());
        File cachedETagsFile = new File(downloadTaskDir, "etags.json");
        assertEquals(cachedETagsFile.getCanonicalFile(), t.getCachedETagsFile());

        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                cachedETagsFile, "UTF-8");

        Map<String, Object> expectedETag = new LinkedHashMap<>();
        expectedETag.put("ETag", etag);

        Map<String, Object> expectedHost = new LinkedHashMap<>();
        expectedHost.put("/" + TEST_FILE_NAME, expectedETag);

        Map<String, Object> expectedCachedETags = new LinkedHashMap<>();
        expectedCachedETags.put(wireMockRule.baseUrl(), expectedHost);

        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the downloadTaskDir can be configured
     * @throws Exception if anything goes wrong
     */
    @Test
    public void configureDownloadTaskDir() throws Exception {
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        File newDownloadTaskDir = folder.newFolder();
        t.downloadTaskDir(newDownloadTaskDir);
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);

        assertEquals(newDownloadTaskDir, t.getDownloadTaskDir());
        File cachedETagsFile = new File(newDownloadTaskDir, "etags.json");
        assertTrue(cachedETagsFile.exists());
        assertEquals(cachedETagsFile, t.getCachedETagsFile());
    }

    /**
     * Tests if the cachedETagsFile can be configured
     * @throws Exception if anything goes wrong
     */
    @Test
    public void configureCachedETagsFile() throws Exception {
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        File newCachedETagsFile = folder.newFile();
        assertTrue(newCachedETagsFile.delete());
        t.cachedETagsFile(newCachedETagsFile);
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);

        assertEquals(newCachedETagsFile, t.getCachedETagsFile());
        assertTrue(newCachedETagsFile.exists());
    }

    /**
     * Create a cached ETags file for the given etag
     * @param cachedETagsFile the file to create
     * @throws IOException if the file could not be created
     */
    private void prepareCachedETagsFile(File cachedETagsFile, String etag) throws IOException {
        Map<String, Object> etagMap = new LinkedHashMap<>();
        etagMap.put("ETag", etag);
        Map<String, Object> hostMap = new LinkedHashMap<>();
        hostMap.put("/" + TEST_FILE_NAME, etagMap);
        Map<String, Object> cachedETags = new LinkedHashMap<>();
        cachedETags.put(wireMockRule.baseUrl(), hostMap);
        String cachedETagsContents = JsonOutput.toJson(cachedETags);
        FileUtils.writeStringToFile(cachedETagsFile, cachedETagsContents,
                StandardCharsets.UTF_8);
    }

    /**
     * Tests if the plugin doesn't download a file if the etag equals
     * the cached one
     * @throws Exception if anything goes wrong
     */
    @Test
    public void dontDownloadIfEqual() throws Exception {
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", equalTo(etag))
                .willReturn(aResponse()
                        .withStatus(304)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        prepareCachedETagsFile(t.getCachedETagsFile(), etag);

        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals("Hello", dstContents);
        assertNotEquals(CONTENTS, dstContents);
    }

    /**
     * Tests if the plugin still downloads a file if the cached ETag is correct
     * but the destination file does not exist.
     * @throws Exception if anything goes wrong
     */
    @Test
    public void forceDownloadIfDestNotExists() throws Exception {
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        prepareCachedETagsFile(t.getCachedETagsFile(), etag);

        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Tests if the plugin downloads a file if its ETag does not match the
     * cached one
     * @throws Exception if anything goes wrong
     */
    @Test
    public void modifiedDownload() throws Exception {
        String wrongEtag = "\"barfoo\"";
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", equalTo(wrongEtag))
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        prepareCachedETagsFile(t.getCachedETagsFile(), wrongEtag);

        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Tests if the plugin supports weak etags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeWeakETagIssueWarning() throws Exception {
        String etag = "W/\"foobar1\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        assertEquals(Boolean.TRUE, t.getUseETag());
        t.compress(false);
        t.execute();

        // check server response
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);

        // read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        // check cached etags
        Map<String, Object> expectedETag = new LinkedHashMap<>();
        expectedETag.put("ETag", etag);
        Map<String, Object> expectedHost = new LinkedHashMap<>();
        expectedHost.put("/" + TEST_FILE_NAME, expectedETag);
        Map<String, Object> expectedCachedETags = new LinkedHashMap<>();
        expectedCachedETags.put(wireMockRule.baseUrl(), expectedHost);
        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the plugin supports weak etags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeAllETags() throws Exception {
        String etag = "W/\"foobar1\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag("all");
        assertEquals("all", t.getUseETag());
        t.compress(false);
        t.execute();

        // check server response
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);

        // read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        // check cached etags
        Map<String, Object> expectedETag = new LinkedHashMap<>();
        expectedETag.put("ETag", etag);
        Map<String, Object> expectedHost = new LinkedHashMap<>();
        expectedHost.put("/" + TEST_FILE_NAME, expectedETag);
        Map<String, Object> expectedCachedETags = new LinkedHashMap<>();
        expectedCachedETags.put(wireMockRule.baseUrl(), expectedHost);
        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the plugin can ignore weak ETags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeStrongOnly() throws Exception {
        String etag1 = "W/\"foobar1\"";
        String etag2 = "\"foobar2\"";

        wireMockRule.stubFor(get(urlEqualTo("/file1"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag1)
                        .withBody(CONTENTS + "1")));
        wireMockRule.stubFor(get(urlEqualTo("/file2"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag2)
                        .withBody(CONTENTS + "2")));

        // download first file
        Download t = makeProjectAndTask();
        t.src(wireMockRule.url("file1"));
        File dst1 = folder.newFile();
        assertTrue(dst1.delete());
        assertFalse(dst1.exists());
        t.dest(dst1);
        t.onlyIfModified(true);
        t.useETag("strongOnly");
        assertEquals("strongOnly", t.getUseETag());
        t.compress(false);
        t.execute();

        // download second file
        t = makeProjectAndTask();
        t.src(wireMockRule.url("file2"));
        File dst2 = folder.newFile();
        assertTrue(dst2.delete());
        assertFalse(dst2.exists());
        t.dest(dst2);
        t.onlyIfModified(true);
        t.useETag("strongOnly");
        assertEquals("strongOnly", t.getUseETag());
        t.compress(false);
        t.execute();

        // check server responses
        String dst1Contents = FileUtils.readFileToString(dst1,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS + "1", dst1Contents);
        String dst2Contents = FileUtils.readFileToString(dst2,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS + "2", dst2Contents);

        // read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        // check cached etags (there should be no entry for etag1)
        Map<String, Object> expectedETag2 = new LinkedHashMap<>();
        expectedETag2.put("ETag", etag2);

        Map<String, Object> expectedHost = new LinkedHashMap<>();
        expectedHost.put("/file2", expectedETag2);

        Map<String, Object> expectedCachedETags = new LinkedHashMap<>();
        expectedCachedETags.put(wireMockRule.baseUrl(), expectedHost);

        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Make sure we cannot assign an invalid value to the "useETag" flag
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidUseETagFlag() {
        Download t = makeProjectAndTask();
        t.useETag("foobar");
    }
}

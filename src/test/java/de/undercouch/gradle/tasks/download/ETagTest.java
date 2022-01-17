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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst).doesNotExist();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        assertThat(t.getCachedETagsFile()).doesNotExist();
    }

    /**
     * Tests if the plugin can handle an incorrect (unquoted) ETag header and
     * still downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectETag() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", "abcd")
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst).doesNotExist();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
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

        stubFor(get(urlEqualTo("/file1"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag1)
                        .withBody(CONTENTS + "1")));
        stubFor(get(urlEqualTo("/file2"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag2)
                        .withBody(CONTENTS + "2")));

        // download first file
        Download t = makeProjectAndTask();
        t.src(wireMock.url("file1"));
        File dst1 = newTempFile();
        assertThat(dst1.delete()).isTrue();
        assertThat(dst1).doesNotExist();
        t.dest(dst1);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        execute(t);

        // download second file
        t = makeProjectAndTask();
        t.src(wireMock.url("file2"));
        File dst2 = newTempFile();
        assertThat(dst2.delete()).isTrue();
        assertThat(dst2).doesNotExist();
        t.dest(dst2);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        execute(t);

        // check server responses
        assertThat(dst1).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS + "1");
        assertThat(dst2).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS + "2");

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
        expectedCachedETags.put(wireMock.baseUrl(), expectedHost);

        assertThat(cachedETags).isEqualTo(expectedCachedETags);
    }

    /**
     * Tests if the plugin downloads a file and stores the etag correctly to
     * the default cached etags file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeETag() throws Exception {
        String etag = "\"foobar\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst).doesNotExist();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        assertThat((Boolean)t.getUseETag()).isTrue();
        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        File buildDir = new File(projectDir, "build");
        File downloadTaskDir = new File(buildDir, "download-task");
        assertThat(t.getDownloadTaskDir()).isEqualTo(downloadTaskDir.getCanonicalFile());
        File cachedETagsFile = new File(downloadTaskDir, "etags.json");
        assertThat(t.getCachedETagsFile()).isEqualTo(cachedETagsFile.getCanonicalFile());

        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                cachedETagsFile, "UTF-8");

        Map<String, Object> expectedETag = new LinkedHashMap<>();
        expectedETag.put("ETag", etag);

        Map<String, Object> expectedHost = new LinkedHashMap<>();
        expectedHost.put("/" + TEST_FILE_NAME, expectedETag);

        Map<String, Object> expectedCachedETags = new LinkedHashMap<>();
        expectedCachedETags.put(wireMock.baseUrl(), expectedHost);

        assertThat(cachedETags).isEqualTo(expectedCachedETags);
    }

    /**
     * Tests if the downloadTaskDir can be configured
     * @throws Exception if anything goes wrong
     */
    @Test
    public void configureDownloadTaskDir() throws Exception {
        String etag = "\"foobar\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        File newDownloadTaskDir = newTempDir();
        t.downloadTaskDir(newDownloadTaskDir);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        assertThat(t.getDownloadTaskDir()).isEqualTo(newDownloadTaskDir);
        File cachedETagsFile = new File(newDownloadTaskDir, "etags.json");
        assertThat(cachedETagsFile).exists();
        assertThat(t.getCachedETagsFile()).isEqualTo(cachedETagsFile);
    }

    /**
     * Tests if the cachedETagsFile can be configured
     * @throws Exception if anything goes wrong
     */
    @Test
    public void configureCachedETagsFile() throws Exception {
        String etag = "\"foobar\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        File newCachedETagsFile = newTempFile();
        assertThat(newCachedETagsFile.delete()).isTrue();
        t.cachedETagsFile(newCachedETagsFile);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        assertThat(t.getCachedETagsFile()).isEqualTo(newCachedETagsFile);
        assertThat(newCachedETagsFile).exists();
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
        cachedETags.put(wireMock.baseUrl(), hostMap);
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

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", equalTo(etag))
                .willReturn(aResponse()
                        .withStatus(304)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        prepareCachedETagsFile(t.getCachedETagsFile(), etag);

        t.compress(false);
        execute(t);

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertThat(dstContents).isEqualTo("Hello");
        assertThat(dstContents).isNotEqualTo(CONTENTS);
    }

    /**
     * Tests if the plugin still downloads a file if the cached ETag is correct
     * but the destination file does not exist.
     * @throws Exception if anything goes wrong
     */
    @Test
    public void forceDownloadIfDestNotExists() throws Exception {
        String etag = "\"foobar\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        prepareCachedETagsFile(t.getCachedETagsFile(), etag);

        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
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

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", equalTo(wrongEtag))
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        prepareCachedETagsFile(t.getCachedETagsFile(), wrongEtag);

        t.compress(false);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if the plugin supports weak etags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeWeakETagIssueWarning() throws Exception {
        String etag = "W/\"foobar1\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst).doesNotExist();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        assertThat(t.getUseETag()).isEqualTo(true);
        t.compress(false);
        execute(t);

        // check server response
        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

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
        expectedCachedETags.put(wireMock.baseUrl(), expectedHost);
        assertThat(cachedETags).isEqualTo(expectedCachedETags);
    }

    /**
     * Tests if the plugin supports weak etags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeAllETags() throws Exception {
        String etag = "W/\"foobar1\"";

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst).doesNotExist();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag("all");
        assertThat(t.getUseETag()).isEqualTo("all");
        t.compress(false);
        execute(t);

        // check server response
        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

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
        expectedCachedETags.put(wireMock.baseUrl(), expectedHost);
        assertThat(cachedETags).isEqualTo(expectedCachedETags);
    }

    /**
     * Tests if the plugin can ignore weak ETags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeStrongOnly() throws Exception {
        String etag1 = "W/\"foobar1\"";
        String etag2 = "\"foobar2\"";

        stubFor(get(urlEqualTo("/file1"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag1)
                        .withBody(CONTENTS + "1")));
        stubFor(get(urlEqualTo("/file2"))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag2)
                        .withBody(CONTENTS + "2")));

        // download first file
        Download t = makeProjectAndTask();
        t.src(wireMock.url("file1"));
        File dst1 = newTempFile();
        assertThat(dst1.delete()).isTrue();
        assertThat(dst1).doesNotExist();
        t.dest(dst1);
        t.onlyIfModified(true);
        t.useETag("strongOnly");
        assertThat(t.getUseETag()).isEqualTo("strongOnly");
        t.compress(false);
        execute(t);

        // download second file
        t = makeProjectAndTask();
        t.src(wireMock.url("file2"));
        File dst2 = newTempFile();
        assertThat(dst2.delete()).isTrue();
        assertThat(dst2).doesNotExist();
        t.dest(dst2);
        t.onlyIfModified(true);
        t.useETag("strongOnly");
        assertThat(t.getUseETag()).isEqualTo("strongOnly");
        t.compress(false);
        execute(t);

        // check server responses
        assertThat(dst1).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS + "1");
        assertThat(dst2).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS + "2");

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
        expectedCachedETags.put(wireMock.baseUrl(), expectedHost);

        assertThat(cachedETags).isEqualTo(expectedCachedETags);
    }

    /**
     * Make sure we cannot assign an invalid value to the "useETag" flag
     */
    @Test
    public void invalidUseETagFlag() {
        Download t = makeProjectAndTask();
        assertThatThrownBy(() -> t.useETag("foobar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal value for 'useETag' flag");
    }
}

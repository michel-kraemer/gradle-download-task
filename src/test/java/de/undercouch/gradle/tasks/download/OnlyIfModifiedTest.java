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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if the plugin handles the last-modified header correctly
 * @author Michel Kraemer
 */
public class OnlyIfModifiedTest extends TestBaseWithMockServer {
    private static final String LAST_MODIFIED = "/last-modified";

    /**
     * Tests if the plugin can handle a missing Last-Modified header and still
     * downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingLastModified() throws Exception {
        stubFor(get(urlEqualTo(LAST_MODIFIED))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(LAST_MODIFIED));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst.exists()).isFalse();
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if the plugin can handle an incorrect Last-Modified header and
     * still downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectLastModified() throws Exception {
        stubFor(get(urlEqualTo(LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "abcd")
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(LAST_MODIFIED));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst.exists()).isFalse();
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if the plugin downloads a file and sets the timestamp correctly
     * @throws Exception if anything goes wrong
     */
    @Test
    public void setFileLastModified() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();

        stubFor(get(urlEqualTo(LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(LAST_MODIFIED));
        File dst = newTempFile();
        assertThat(dst.delete()).isTrue();
        assertThat(dst.exists()).isFalse();
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        assertThat(dst.exists()).isTrue();
        long lmlong = dst.lastModified();
        assertThat(lmlong).isEqualTo(expectedlmlong);
        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if the plugin doesn't download a file if the timestamp equals
     * the last-modified header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void dontDownloadIfEqual() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();

        stubFor(get(urlEqualTo(LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(LAST_MODIFIED));
        File dst = newTempFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        assertThat(dst.setLastModified(expectedlmlong)).isTrue();
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        long lmlong = dst.lastModified();
        assertThat(lmlong).isEqualTo(expectedlmlong);
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertThat(dstContents).isEqualTo("Hello");
        assertThat(dstContents).isNotEqualTo(CONTENTS);
    }

    /**
     * Tests if the plugin doesn't download a file if the timestamp is newer
     * than the last-modified header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void dontDownloadIfOlder() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();

        stubFor(get(urlEqualTo(LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(LAST_MODIFIED));
        File dst = newTempFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        assertThat(dst.setLastModified(expectedlmlong + 1000)).isTrue();
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        long lmlong = dst.lastModified();
        assertThat(lmlong).isEqualTo(expectedlmlong + 1000);
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertThat(dstContents).isEqualTo("Hello");
        assertThat(dstContents).isNotEqualTo(CONTENTS);
    }

    /**
     * Tests if the plugin downloads a file if the timestamp is older than
     * the last-modified header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void newerDownload() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();

        stubFor(get(urlEqualTo(LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(LAST_MODIFIED));
        File dst = newTempFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        assertThat(CONTENTS).isNotEqualTo("Hello");
        assertThat(dst.setLastModified(expectedlmlong - 1000)).isTrue();
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        long lmlong = dst.lastModified();
        assertThat(lmlong).isEqualTo(expectedlmlong);
        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Make sure 'onlyIfNewer' is a proper alias for 'onlyIfModified'
     */
    @Test
    public void alias() {
        Download t = makeProjectAndTask();
        assertThat(t.isOnlyIfModified()).isFalse();
        assertThat(t.isOnlyIfNewer()).isFalse();
        t.onlyIfModified(true);
        assertThat(t.isOnlyIfModified()).isTrue();
        assertThat(t.isOnlyIfNewer()).isTrue();
        t.onlyIfNewer(false);
        assertThat(t.isOnlyIfModified()).isFalse();
        assertThat(t.isOnlyIfNewer()).isFalse();
    }
}

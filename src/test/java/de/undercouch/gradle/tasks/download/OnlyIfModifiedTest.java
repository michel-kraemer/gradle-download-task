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
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests if the plugin handles the last-modified header correctly
 * @author Michel Kraemer
 */
public class OnlyIfModifiedTest extends TestBaseWithMockServer {
    private static final String LAST_MODIFIED = "last-modified";

    /**
     * Tests if the plugin can handle a missing Last-Modified header and still
     * downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingLastModified() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + LAST_MODIFIED))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(LAST_MODIFIED));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Tests if the plugin can handle an incorrect Last-Modified header and
     * still downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectLastModified() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "abcd")
                        .withBody(CONTENTS)));
        
        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(LAST_MODIFIED));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
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

        wireMockRule.stubFor(get(urlEqualTo("/" + LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(LAST_MODIFIED));
        File dst = folder.newFile();
        assertTrue(dst.delete());
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        assertTrue(dst.exists());
        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong, lmlong);
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
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

        wireMockRule.stubFor(get(urlEqualTo("/" + LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(LAST_MODIFIED));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        assertTrue(dst.setLastModified(expectedlmlong));
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong, lmlong);
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals("Hello", dstContents);
        assertNotEquals(CONTENTS, dstContents);
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

        wireMockRule.stubFor(get(urlEqualTo("/" + LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(LAST_MODIFIED));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        assertTrue(dst.setLastModified(expectedlmlong + 1000));
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong + 1000, lmlong);
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals("Hello", dstContents);
        assertNotEquals(CONTENTS, dstContents);
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

        wireMockRule.stubFor(get(urlEqualTo("/" + LAST_MODIFIED))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(LAST_MODIFIED));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello", StandardCharsets.UTF_8);
        assertNotEquals("Hello", CONTENTS);
        assertTrue(dst.setLastModified(expectedlmlong - 1000));
        t.dest(dst);
        t.onlyIfModified(true);
        execute(t);

        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong, lmlong);
        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Make sure 'onlyIfNewer' is a proper alias for 'onlyIfModified'
     */
    @Test
    public void alias() {
        Download t = makeProjectAndTask();
        assertFalse(t.isOnlyIfModified());
        assertFalse(t.isOnlyIfNewer());
        t.onlyIfModified(true);
        assertTrue(t.isOnlyIfModified());
        assertTrue(t.isOnlyIfNewer());
        t.onlyIfNewer(false);
        assertFalse(t.isOnlyIfModified());
        assertFalse(t.isOnlyIfNewer());
    }
}

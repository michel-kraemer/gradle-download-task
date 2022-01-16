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

import org.apache.hc.core5.http.ConnectionClosedException;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests if the plugin can handle invalid or missing Content-Length header
 * @author Michel Kraemer
 */
public class ContentLengthTest extends TestBaseWithMockServer {
    /**
     * Tests if the plugin can handle a missing Content-Length header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingContentLength() throws Exception {
        String testFileName = "/test.txt";
        String contents = "Hello";

        stubFor(get(urlEqualTo(testFileName))
                .willReturn(aResponse()
                        .withBody(contents)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(testFileName));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(contents);
    }

    /**
     * Tests if the plugin can handle an incorrect Content-Length header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void correctContentLength() throws Exception {
        String testFileName = "/test.txt";
        String contents = "Hello";

        stubFor(get(urlEqualTo(testFileName))
                .willReturn(aResponse()
                        .withHeader("content-length", "5")
                        .withBody(contents)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(testFileName));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(contents);
    }

    /**
     * Tests if the plugin can handle an incorrect Content-Length header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void tooLargeContentLength() throws Exception {
        String testFileName = "/test.txt";
        String contents = "Hello";

        stubFor(get(urlEqualTo(testFileName))
                .willReturn(aResponse()
                        .withHeader("content-length", "10000")
                        .withBody(contents)));

        Download t = makeProjectAndTask();
        t.compress(false); // do not use GZIP or the response will be chunked
        t.src(wireMock.url(testFileName));
        File dst = newTempFile();
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .hasRootCauseInstanceOf(ConnectionClosedException.class);
    }
}

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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if HTTP headers can be sent
 * @author Michel Kraemer
 */
public class HeadersTest extends TestBaseWithMockServer {
    private static final String X_HEADER_TEST_A = "X-Header-Test-A";
    private static final String X_HEADER_TEST_B = "X-Header-Test-B";
    private static final String VALUE_A = "value A";
    private static final String VALUE_B = "value B";

    /**
     * Tests that no headers request headers are set when not specified
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithNoHeaders() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader(X_HEADER_TEST_A, absent())
                .withHeader(X_HEADER_TEST_B, absent())
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests that specified request headers are included
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithHeaders() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader(X_HEADER_TEST_A, equalTo(VALUE_A))
                .withHeader(X_HEADER_TEST_B, equalTo(VALUE_B))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        t.header(X_HEADER_TEST_A, VALUE_A);
        t.header(X_HEADER_TEST_B, VALUE_B);
        execute(t);

        assertThat(t.getHeaders()).hasSize(2);
        assertThat(t.getHeader(X_HEADER_TEST_A)).isEqualTo(VALUE_A);
        assertThat(t.getHeader(X_HEADER_TEST_B)).isEqualTo(VALUE_B);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests that request headers can be set
     */
    @Test
    public void downloadWithHeadersMap() {
        Download t = makeProjectAndTask();

        assertThat(t.getHeader(X_HEADER_TEST_A)).isNull();

        Map<String, String> headers = new HashMap<>();
        headers.put(X_HEADER_TEST_A, VALUE_A);
        headers.put(X_HEADER_TEST_B, VALUE_B);
        t.headers(headers);

        assertThat(t.getHeaders()).hasSize(2);
        assertThat(t.getHeader(X_HEADER_TEST_A)).isEqualTo(VALUE_A);
        assertThat(t.getHeader(X_HEADER_TEST_B)).isEqualTo(VALUE_B);

        t.headers(null);
        assertThat(t.getHeaders()).isEmpty();
    }
}

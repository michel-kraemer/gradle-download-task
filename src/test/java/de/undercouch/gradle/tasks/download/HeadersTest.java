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
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader(X_HEADER_TEST_A, absent())
                .withHeader(X_HEADER_TEST_B, absent())
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        execute(t);

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Tests that specified request headers are included
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithHeaders() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader(X_HEADER_TEST_A, equalTo(VALUE_A))
                .withHeader(X_HEADER_TEST_B, equalTo(VALUE_B))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        t.header(X_HEADER_TEST_A, VALUE_A);
        t.header(X_HEADER_TEST_B, VALUE_B);
        execute(t);

        assertEquals(2, t.getHeaders().size());
        assertEquals(VALUE_A, t.getHeader(X_HEADER_TEST_A));
        assertEquals(VALUE_B, t.getHeader(X_HEADER_TEST_B));

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Tests that request headers can be set
     */
    @Test
    public void downloadWithHeadersMap() {
        Download t = makeProjectAndTask();

        assertNull(t.getHeader(X_HEADER_TEST_A));

        Map<String, String> headers = new HashMap<>();
        headers.put(X_HEADER_TEST_A, VALUE_A);
        headers.put(X_HEADER_TEST_B, VALUE_B);
        t.headers(headers);

        assertEquals(2, t.getHeaders().size());
        assertEquals(VALUE_A, t.getHeader(X_HEADER_TEST_A));
        assertEquals(VALUE_B, t.getHeader(X_HEADER_TEST_B));

        t.headers(null);
        assertEquals(0, t.getHeaders().size());
    }
}

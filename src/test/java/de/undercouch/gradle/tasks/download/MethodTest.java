// Copyright 2013-2023 Michel Kraemer
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

import com.github.tomakehurst.wiremock.matching.MatchResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests if another HTTP method can be used
 * @author Michel Kraemer
 */
public class MethodTest extends TestBaseWithMockServer {
    private void testWithMethod(String method) throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        t.method(method);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests that we can download a file using HTTP POST
     * @throws Exception if anything goes wrong
     */
    @Test
    public void postMethod() throws Exception {
        stubFor(post(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));
        testWithMethod("POST");
    }

    /**
     * Tests that we can download a file using HTTP PUT
     * @throws Exception if anything goes wrong
     */
    @Test
    public void putMethod() throws Exception {
        stubFor(put(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));
        testWithMethod("PUT");
    }

    /**
     * Tests that we can download a file using HTTP POST even if we specify
     * the method in mixed case
     * @throws Exception if anything goes wrong
     */
    @Test
    public void caseInsensitive() throws Exception {
        stubFor(post(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));
        testWithMethod("poSt");
    }

    /**
     * Tests that we can download a file using a custom HTTP method
     * the method in mixed case
     * @throws Exception if anything goes wrong
     */
    @Test
    public void customMethod() throws Exception {
        stubFor(requestMatching(r -> {
            if (r.getMethod().getName().equals("CUSTOM") &&
                    r.getUrl().equals("/" + TEST_FILE_NAME)) {
                return MatchResult.exactMatch();
            }
            return MatchResult.noMatch();
        }).willReturn(aResponse().withBody(CONTENTS)));
        testWithMethod("custom");
    }

    /**
     * Makes sure {@code null} cannot be used as method
     * @throws Exception if anything goes wrong
     */
    @Test
    public void nullMethod() throws Exception {
        Download t = makeProjectAndTask();
        assertThat(t.getMethod()).isEqualTo("GET");
        assertThatThrownBy(() -> t.method(null)).isInstanceOf(
                IllegalArgumentException.class);
    }
}

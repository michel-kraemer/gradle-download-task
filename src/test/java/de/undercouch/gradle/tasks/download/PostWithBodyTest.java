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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if another we can POST a request body
 * @author Michel Kraemer
 */
public class PostWithBodyTest extends TestBaseWithMockServer {
    /**
     * Tests that we can download a file using HTTP POST and a body
     * @throws Exception if anything goes wrong
     */
    @Test
    public void postWithBody() throws Exception {
        String body = "This is a body";

        stubFor(post(urlEqualTo("/" + TEST_FILE_NAME))
                .withRequestBody(equalTo(body))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        t.method("POST");
        t.body(body);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }
}

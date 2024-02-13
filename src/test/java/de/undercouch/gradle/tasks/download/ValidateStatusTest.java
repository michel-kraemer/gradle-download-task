// Copyright 2013-2024 Michel Kraemer
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

import org.apache.hc.client5.http.ClientProtocolException;
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
 * Tests if we can add a custom status validator that accepts HTTP error
 * status codes
 * @author Michel Kraemer
 */
public class ValidateStatusTest extends TestBaseWithMockServer {
    /**
     * Tests if the download fails if there is no validator (default) and the
     * server returns status code 400
     * @throws Exception if anything goes wrong
     */
    @Test
    public void noValidator() throws Exception {
        String testFileName = "/error.txt";
        String contents = "This is a very bad request";

        stubFor(get(urlEqualTo(testFileName))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody(contents)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(testFileName));
        File dst = newTempFile();
        t.dest(dst);

        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .rootCause()
                .isInstanceOf(ClientProtocolException.class)
                .hasMessageContaining("Bad Request");
    }

    /**
     * Tests if we can accept status code 400
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validateBadRequest() throws Exception {
        String testFileName = "/error.txt";
        String contents = "This is a very bad request";

        stubFor(get(urlEqualTo(testFileName))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody(contents)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(testFileName));
        File dst = newTempFile();
        t.dest(dst);
        t.validateStatus(code -> code == 400);
        execute(t);

        assertThat(dst)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(contents);
    }
}

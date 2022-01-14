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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if the plugin can handle invalid or missing Content-Encoding header.
 * See https://github.com/michel-kraemer/gradle-download-task/issues/55
 * @author Michel Kraemer
 */
public class ContentEncodingTest extends TestBaseWithMockServer {
    private static final byte[] CONTENTS_BYTES = new byte[] { 0x00, 0x01 };

    /**
     * Tests if the plugin can handle the invalid value 'none'
     * @throws Exception if anything goes wrong
     */
    @Test
    public void contentEncodingNone() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Content-Encoding", "None")
                        .withHeader("Content-Length", String.valueOf(CONTENTS_BYTES.length))
                        .withBody(CONTENTS_BYTES)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).hasBinaryContent(CONTENTS_BYTES);
    }
}

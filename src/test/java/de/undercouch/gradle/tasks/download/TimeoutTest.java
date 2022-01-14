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

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.gradle.api.UncheckedIOException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.SocketTimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

/**
 * Tests if the task times out if the response takes too long
 * @author Michel Kraemer
 */
public class TimeoutTest extends TestBaseWithMockServer {
    private static final int TIMEOUT_MS = 100;
    private static final String TIMEOUT = "/timeout";

    /**
     * Tests that the task times out if the response takes too long
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void readTimeout() throws Exception {
        stubFor(get(urlEqualTo(TIMEOUT))
                .willReturn(aResponse()
                        .withFixedDelay(TIMEOUT_MS * 10)
                        .withBody("Whatever")));

        Download t = makeProjectAndTask();
        t.readTimeout(TIMEOUT_MS);
        assertThat(t.getReadTimeout()).isEqualTo(TIMEOUT_MS);
        t.src(wireMock.url(TIMEOUT));
        File dst = newTempFile();
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    /**
     * Tests that the task times out if takes too long to connect to the server
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void connectTimeout() throws Exception {
        Download t = makeProjectAndTask();
        t.connectTimeout(TIMEOUT_MS);
        assertThat(t.getConnectTimeout()).isEqualTo(TIMEOUT_MS);
        t.src("http://10.255.255.1"); // try to connect to an invalid host
        File dst = newTempFile();
        t.dest(dst);
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(ConnectTimeoutException.class);
        long end = System.currentTimeMillis();
        assertThat(end).isCloseTo(start, offset(TIMEOUT_MS * 2L));
    }
}

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

import com.github.tomakehurst.wiremock.http.trafficlistener.DoNothingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests if a file can be downloaded to a temporary location and then
 * moved to the final location
 * @author Michel Kraemer
 */
public class TempAndMoveTest extends TestBase {
    private static final String TEMPANDMOVE = "tempandmove";
    private static final String CONTENTS = "Hello world";
    private File dst;
    private File downloadTaskDir;
    private boolean checkDstDoesNotExist;

    /**
     * Run a mock HTTP server with a network listener
     */
    @RegisterExtension
    public WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .networkTrafficListener(new TempAndMoveNetworkTrafficListener())
                    .jettyStopTimeout(10000L))
            .configureStaticDsl(true)
            .build();

    private class TempAndMoveNetworkTrafficListener extends
            DoNothingWiremockNetworkTrafficListener {
        @Override
        public void opened(Socket socket) {
            // at the beginning there should be no dest file and no temp file
            if (checkDstDoesNotExist) {
                assertThat(dst).doesNotExist();
            }
            assertThat(getTempFile()).isNull();
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer bytes) {
            // wait for temporary file (max. 10 seconds)
            File tempFile = null;
            for (int i = 0; i < 100; ++i) {
                tempFile = getTempFile();
                if (tempFile != null) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // temp file should now exist, but dest file not
            if (checkDstDoesNotExist) {
                assertThat(dst).doesNotExist();
            }
            assertThat(tempFile).isNotNull();
        }
    }

    private File getTempFile() {
        File[] files = downloadTaskDir.listFiles((dir, name) ->
                name.startsWith(dst.getName()) && name.endsWith(".part"));

        if (files == null) {
            // downloadTaskDir does not exist
            return null;
        }
        if (files.length > 1) {
            fail("Multiple temp files in " + downloadTaskDir);
        }

        return files.length == 1 ? files[0] : null;
    }

    private void testTempAndMove(boolean createDst) throws Exception {
        stubFor(get(urlEqualTo("/" + TEMPANDMOVE))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        downloadTaskDir = t.getDownloadTaskDir();
        String src = wireMock.url(TEMPANDMOVE);
        t.src(src);
        dst = newTempFile();

        checkDstDoesNotExist = !createDst;

        if (createDst) {
            dst = newTempFile();
            OutputStream os = Files.newOutputStream(dst.toPath());
            os.close();
        } else {
            // make sure dest does not exist, so we can verify correctly
            assertThat(dst.delete()).isTrue();
        }

        t.dest(dst);
        t.tempAndMove(true);
        execute(t);

        assertThat(dst).exists();
        assertThat(getTempFile()).isNull();

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if a file can be downloaded to a temporary location and then
     * moved to the final location
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void tempAndMove() throws Exception {
        testTempAndMove(false);
    }

    /**
     * Tests if a file can be downloaded to a temporary location and then
     * moved to the final location overwriting existing file
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void tempAndMoveOverwrite() throws Exception {
        testTempAndMove(true);
    }
}

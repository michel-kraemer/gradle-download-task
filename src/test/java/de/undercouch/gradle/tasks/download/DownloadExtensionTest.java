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

import org.apache.hc.client5.http.ClientProtocolException;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests {@link DownloadExtension}
 * @author Michel Kraemer
 */
public class DownloadExtensionTest extends TestBaseWithMockServer {
    /**
     * Create a WireMock stub for {@link #TEST_FILE_NAME} with {@link #CONTENTS}
     */
    @BeforeEach
    public void stubForTestFile() {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("content-length", String.valueOf(CONTENTS.length()))
                        .withBody(CONTENTS)));
    }

    /**
     * Download a file using the {@link DownloadExtension}
     * @param project a Gradle project
     * @param src the file to download
     * @param dst the download destination
     * @param async {@code true} if the download should be executed asynchronously
     */
    private void doDownload(Project project, final String src, final File dst,
            boolean async) {
        DownloadExtension e = new DownloadExtension(project);
        Action<DownloadSpec> a = action -> {
            try {
                action.src(src);
                assertThat(action.getSrc()).isInstanceOf(URL.class);
                assertThat(action.getSrc().toString()).isEqualTo(src);
                action.dest(dst);
                assertThat(action.getDest()).isSameAs(dst);
            } catch (IOException t) {
                fail("Could not execute action", t);
            }
        };

        if (!async) {
            e.run(a);
        } else {
            e.runAsync(a).join();
        }
    }

    /**
     * Tests if a single file can be downloaded
     * @throws Exception if anything goes wrong
     */
    @ParameterizedTest(name = "async = {0}")
    @ValueSource(booleans = { true, false })
    public void downloadSingleFile(boolean async) throws Exception {
        Download t = makeProjectAndTask();

        String src = wireMock.url(TEST_FILE_NAME);
        File dst = newTempFile();

        doDownload(t.getProject(), src, dst, async);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if the download fails if the file does not exist
     * @throws Exception if anything goes wrong
     */
    @ParameterizedTest(name = "async = {0}")
    @ValueSource(booleans = { true, false })
    public void downloadSingleFileError(boolean async) throws Exception {
        stubFor(get(urlEqualTo("/foobar.txt"))
                .willReturn(aResponse().withStatus(404)));

        Download t = makeProjectAndTask();
        String src = wireMock.url("foobar.txt");
        File dst = newTempFile();
        if (!async) {
            assertThatThrownBy(() -> doDownload(t.getProject(), src, dst, false))
                    .isInstanceOf(IllegalStateException.class)
                    .rootCause()
                    .isInstanceOf(ClientProtocolException.class)
                    .hasMessageContaining("HTTP status code: 404");
        } else {
            assertThatThrownBy(() -> doDownload(t.getProject(), src, dst, true))
                    .isInstanceOf(CompletionException.class)
                    .rootCause()
                    .isInstanceOf(ClientProtocolException.class)
                    .hasMessageContaining("HTTP status code: 404");
        }
    }

    /**
     * Tests if the download extension can be created through the object factory
     * for a task. See issue #284 for more information.
     * @throws Exception if anything goes wrong
     */
    @Test
    public void createDownloadExtensionForTask() throws Exception {
        Download t = makeProjectAndTask();

        String src = wireMock.url(TEST_FILE_NAME);
        File dst = newTempFile();

        DownloadExtension e = t.getProject().getObjects().newInstance(DownloadExtension.class, t);
        e.run(action -> {
            try {
                action.src(src);
                action.dest(dst);
            } catch (IOException ex) {
                fail("Could not execute action", ex);
            }
        });

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }
}

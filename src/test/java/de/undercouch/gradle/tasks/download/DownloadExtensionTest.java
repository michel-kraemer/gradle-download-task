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

import groovy.lang.Closure;
import org.apache.hc.client5.http.ClientProtocolException;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
     */
    private void doDownload(Project project, final String src, final File dst) {
        DownloadExtension e = new DownloadExtension(project);
        e.configure(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -7729300978830802384L;

            @SuppressWarnings("unused")
            public void doCall() {
                DownloadAction action = (DownloadAction)this.getDelegate();
                action.src(src);
                assertThat(action.getSrc()).isInstanceOf(URL.class);
                assertThat(action.getSrc().toString()).isEqualTo(src);
                action.dest(dst);
                assertThat(action.getDest()).isSameAs(dst);
            }
        });
    }

    /**
     * Tests if a single file can be downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        Download t = makeProjectAndTask();

        String src = wireMock.url(TEST_FILE_NAME);
        File dst = newTempFile();

        doDownload(t.getProject(), src, dst);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if the download fails if the file does not exist
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFileError() throws Exception {
        stubFor(get(urlEqualTo("/foobar.txt"))
                .willReturn(aResponse().withStatus(404)));

        Download t = makeProjectAndTask();
        String src = wireMock.url("foobar.txt");
        File dst = newTempFile();
        assertThatThrownBy(() -> doDownload(t.getProject(), src, dst))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseInstanceOf(ClientProtocolException.class);
    }
}

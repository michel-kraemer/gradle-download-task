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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPathBuilderException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests if the plugin can handle HTTPS
 * @author Michel Kraemer
 */
public class SslTest extends TestBase {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicHttpsPort()
                    .keystorePath(SslTest.class.getResource("/keystore").toString())
                    .keystorePassword("gradle")
                    .keyManagerPassword("gradle")
                    .jettyStopTimeout(10000L))
            .configureStaticDsl(true)
            .build();

    /**
     * Tests if the plugin can fetch a resource from an HTTPS URL accepting
     * any certificate
     * @throws Exception if anything goes wrong
     */
    @Test
    public void acceptAnyCertificate() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        t.acceptAnyCertificate(true);
        assertThat(t.isAcceptAnyCertificate()).isTrue();
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    /**
     * Tests if connecting to an HTTPS URL fails if the certificate is unknown
     * @throws Exception if anything goes wrong
     */
    @Test
    public void unknownCertificate() throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        assertThat(t.isAcceptAnyCertificate()).isFalse();
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .getRootCause()
                .isInstanceOf(CertPathBuilderException.class)
                .hasMessageContaining("unable to find valid certification path");
    }
}

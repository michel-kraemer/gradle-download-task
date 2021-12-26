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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests if the plugin can handle HTTPS
 * @author Michel Kraemer
 */
public class SslTest extends TestBase {
    /**
     * Run a mock HTTP server
     */
    @Rule
    public WireMockRule sslWireMockRule = new WireMockRule(options()
            .dynamicHttpsPort()
            .keystorePath(this.getClass().getResource("/keystore").toString())
            .keystorePassword("gradle")
            .jettyStopTimeout(10000L));

    /**
     * Tests if the plugin can fetch a resource from a HTTPS URL accepting
     * any certificate
     * @throws Exception if anything goes wrong
     */
    @Test
    public void acceptAnyCertificate() throws Exception {
        sslWireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(sslWireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        t.acceptAnyCertificate(true);
        assertTrue(t.isAcceptAnyCertificate());
        t.execute();

        String dstContents = FileUtils.readFileToString(dst, StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }
    
    /**
     * Tests if connecting to a HTTPS URL fails if the certificate is unknown
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void unknownCertificate() throws Exception {
        Download t = makeProjectAndTask();
        t.src(sslWireMockRule.url(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        assertFalse(t.isAcceptAnyCertificate());
        t.execute();
    }
}

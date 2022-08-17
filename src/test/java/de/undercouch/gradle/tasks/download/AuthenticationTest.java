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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.assertj.core.api.Assertions;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests if the plugin can access a resource that requires authentication
 * @author Michel Kraemer
 */
public class AuthenticationTest extends TestBaseWithMockServer {
    private static final String PASSWORD = "testpass456";
    private static final String USERNAME = "testuser123";
    private static final String AUTHENTICATE = "/authenticate";
    private static final String REALM = "Gradle";
    private static final String NONCE = "ABCDEF0123456789";

    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test
    public void noAuthorization() throws Exception {
        stubFor(get(urlEqualTo(AUTHENTICATE))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)
                        .withHeader("WWW-Authenticate", "Basic")));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(AUTHENTICATE));
        File dst = newTempFile();
        t.dest(dst);

        Assertions.setMaxStackTraceElementsDisplayed(1000);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .rootCause()
                .isInstanceOf(ClientProtocolException.class)
                .hasMessageContaining("HTTP status code: 401")
                .hasMessageNotContaining("Missing WWW-Authenticate");

        // check if non-preemptive authentication request was made
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withoutHeader("Authorization"));
    }

    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test
    public void noAuthorizationNoWWWAuthenticateHeader() throws Exception {
        stubFor(get(urlEqualTo(AUTHENTICATE))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(AUTHENTICATE));
        File dst = newTempFile();
        t.dest(dst);

        Assertions.setMaxStackTraceElementsDisplayed(1000);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .rootCause()
                .isInstanceOf(ClientProtocolException.class)
                .hasMessageContaining("HTTP status code: 401")
                .hasMessageContaining("Missing WWW-Authenticate");

        // check if non-preemptive authentication request was made
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withoutHeader("Authorization"));
    }
    
    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test
    public void invalidCredentials() throws Exception {
        String wrongUser = USERNAME + "!";
        String wrongPass = PASSWORD + "!";
        String ahdr = "Basic " + Base64.encodeBase64String(
                (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        stubFor(get(urlEqualTo(AUTHENTICATE))
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Basic realm=\"" + REALM + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(AUTHENTICATE));
        File dst = newTempFile();
        t.dest(dst);
        t.username(wrongUser);
        t.password(wrongPass);

        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .rootCause()
                .isInstanceOf(ClientProtocolException.class)
                .hasMessageContaining("HTTP status code: 401");

        // check if non-preemptive authentication request was made
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withoutHeader("Authorization"));
    }

    /**
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validUserAndPass() throws Exception {
        String ahdr = "Basic " + Base64.encodeBase64String(
                (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        stubFor(get(urlEqualTo(AUTHENTICATE))
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Basic realm=\"" + REALM + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(AUTHENTICATE));
        File dst = newTempFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        // check if non-preemptive authentication request was made
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withoutHeader("Authorization"));
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withHeader("Authorization", equalTo(ahdr)));
    }

    /**
     * Tests if the plugin can access a protected resource using preemptive
     * Basic authentication
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validPreemptive() throws Exception {
        String ahdr = "Basic " + Base64.encodeBase64String(
                (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        stubFor(get(urlEqualTo(AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(AUTHENTICATE));
        File dst = newTempFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        t.preemptiveAuth(true);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        // check if only one preemptive authentication request was made
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)));
    }

    /**
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validDigest() throws Exception {
        String ha1 = DigestUtils.md5Hex(
                USERNAME + ":" + REALM + ":" + PASSWORD);
        String ha2 = DigestUtils.md5Hex(
                "GET:" + AUTHENTICATE);
        String expectedResponse = DigestUtils.md5Hex(
                ha1 + ":" + NONCE + ":" + ha2);
        String ahdr = "Digest username=\"" + USERNAME + "\", " +
                "realm=\"" + REALM + "\", " +
                "nonce=\"" + NONCE + "\", " +
                "uri=\"" + AUTHENTICATE + "\", " +
                "response=\"" + expectedResponse + "\", " +
                "algorithm=MD5";

        stubFor(get(urlEqualTo(AUTHENTICATE))
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Digest realm=\"" + REALM + "\"," +
                                        "nonce=\"" + NONCE + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(AUTHENTICATE));
        File dst = newTempFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        execute(t);

        // check if non-preemptive authentication request was made
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withoutHeader("Authorization"));
        verify(1, getRequestedFor(urlEqualTo(AUTHENTICATE)).withHeader("Authorization", equalTo(ahdr)));
    }
}

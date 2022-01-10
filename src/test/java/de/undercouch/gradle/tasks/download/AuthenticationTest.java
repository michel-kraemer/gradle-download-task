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
import org.apache.commons.io.FileUtils;
import org.gradle.api.UncheckedIOException;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

/**
 * Tests if the plugin can access a resource that requires authentication
 * @author Michel Kraemer
 */
public class AuthenticationTest extends TestBaseWithMockServer {
    private static final String PASSWORD = "testpass456";
    private static final String USERNAME = "testuser123";
    private static final String AUTHENTICATE = "authenticate";
    private static final String REALM = "Gradle";
    private static final String NONCE = "ABCDEF0123456789";

    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test(expected = UncheckedIOException.class)
    public void noAuthorization() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        execute(t);
    }
    
    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test(expected = UncheckedIOException.class)
    public void invalidCredentials() throws Exception {
        String wrongUser = USERNAME + "!";
        String wrongPass = PASSWORD + "!";
        String ahdr = "Basic " + Base64.encodeBase64String(
                (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Basic realm=\"" + REALM + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(wrongUser);
        t.password(wrongPass);
        execute(t);
    }

    /**
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validUserAndPass() throws Exception {
        String ahdr = "Basic " + Base64.encodeBase64String(
                (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Basic realm=\"" + REALM + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        execute(t);

        String dstContents = FileUtils.readFileToString(dst, StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
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
                "GET:/" + AUTHENTICATE);
        String expectedResponse = DigestUtils.md5Hex(
                ha1 + ":" + NONCE + ":" + ha2);
        String ahdr = "Digest username=\"" + USERNAME + "\", " +
                "realm=\"" + REALM + "\", " +
                "nonce=\"" + NONCE + "\", " +
                "uri=\"/" + AUTHENTICATE + "\", " +
                "response=\"" + expectedResponse + "\", " +
                "algorithm=MD5";

        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Digest realm=\"" + REALM + "\"," +
                                        "nonce=\"" + NONCE + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        wireMockRule.stubFor(get(urlEqualTo("/" + AUTHENTICATE))
                .withHeader("Authorization", equalTo(ahdr))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMockRule.url(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        execute(t);
    }
}

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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.google.common.net.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.CircularRedirectException;
import org.apache.hc.client5.http.RedirectException;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests if the plugin can handle redirects
 * @author Michel Kraemer
 */
public class RedirectTest extends TestBaseWithMockServer {
    private static final String REDIRECT = "redirect";

    /**
     * Run a second mock HTTP server with {@link RedirectTransformer}
     */
    @RegisterExtension
    public WireMockExtension redirectWireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()
                    .extensions(RedirectTransformer.class.getName())
                    .jettyStopTimeout(10000L))
            .build();

    /**
     * Run a third mock HTTP server
     */
    @RegisterExtension
    public WireMockExtension redirectWireMock2 = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()
                    .jettyStopTimeout(10000L))
            .build();

    public static class RedirectTransformer extends ResponseTransformer {
        private Integer redirects = null;

        @Override
        public Response transform(Request request, Response response,
                FileSource files, Parameters parameters) {
            if (redirects == null) {
                redirects = parameters.getInt("redirects");
            }
            String nl;
            if (redirects > 1) {
                redirects--;
                nl = "/" + REDIRECT + "?r=" + redirects;
            } else {
                nl = "/" + TEST_FILE_NAME;
            }
            return Response.Builder.like(response)
                    .but().headers(response.getHeaders()
                            .plus(new HttpHeader("Location", nl)))
                    .build();
        }

        @Override
        public String getName() {
            return "redirect";
        }
    }

    /**
     * Tests if the plugin can handle one redirect
     * @throws Exception if anything goes wrong
     */
    @Test
    public void oneRedirect() throws Exception {
        UrlPattern up1 = urlEqualTo("/" + REDIRECT);
        stubFor(get(up1)
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", wireMock.url(TEST_FILE_NAME))));

        UrlPattern up2 = urlEqualTo("/" + TEST_FILE_NAME);
        stubFor(get(up2)
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(wireMock.url(REDIRECT));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        verify(1, getRequestedFor(up1));
        verify(1, getRequestedFor(up2));
    }

    /**
     * Tests if the plugin can handle ten redirects
     * @throws Exception if anything goes wrong
     */
    @Test
    public void tenRedirect() throws Exception {
        UrlPattern up1 = urlPathEqualTo("/" + REDIRECT);
        redirectWireMock.stubFor(get(up1)
                .withQueryParam("r", matching("[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withTransformer("redirect", "redirects", 10)));

        UrlPattern up2 = urlEqualTo("/" + TEST_FILE_NAME);
        redirectWireMock.stubFor(get(up2)
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.src(redirectWireMock.url(REDIRECT) + "?r=10");
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        redirectWireMock.verify(10, getRequestedFor(up1));
        redirectWireMock.verify(1, getRequestedFor(up2));
    }

   /**
    * Tests if the plugin can handle circular redirects
    * @throws Exception if anything goes wrong
    */
   @Test
   public void circularRedirect() throws Exception {
       UrlPattern up1 = urlPathEqualTo("/" + REDIRECT);
       wireMock.stubFor(get(up1)
               .willReturn(aResponse()
                       .withStatus(HttpServletResponse.SC_FOUND)
                       .withHeader("Location", "/" + REDIRECT)));

       Download t = makeProjectAndTask();
       t.src(wireMock.url(REDIRECT));
       File dst = newTempFile();
       t.dest(dst);
       assertThatThrownBy(() -> execute(t))
               .isInstanceOf(WorkerExecutionException.class)
               .rootCause()
               .isInstanceOf(CircularRedirectException.class)
               .hasMessageContaining("Circular redirect");
   }

    /**
     * Make sure the plugin fails with too many redirects
     * @throws Exception if anything goes wrong
     */
    @Test
    public void tooManyRedirects() throws Exception {
        UrlPattern up1 = urlPathEqualTo("/" + REDIRECT);
        redirectWireMock.stubFor(get(up1)
                .withQueryParam("r", matching("[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withTransformer("redirect", "redirects", 51)));

        Download t = makeProjectAndTask();
        t.src(redirectWireMock.url(REDIRECT) + "?r=52");
        File dst = newTempFile();
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .rootCause()
                .isInstanceOf(RedirectException.class)
                .hasMessage("Maximum redirects (50) exceeded");
    }

    /**
     * Test if sensitive headers are removed from the request after redirecting
     * to another host
     * @throws Exception if anything goes wrong
     */
    @Test
    public void removeSensitiveHeaders() throws Exception {
        configureDefaultStub();

        String authorization = "Bearer Elvis";
        String cookie = "MyCookie";
        String cookie2 = "MyCookie2";
        String proxyAuthorization = "Bearer Einar";
        String wwwAuthenticate = "Max";

        // headers should be present on the first request to the first host
        UrlPattern up1 = urlPathEqualTo("/auth");
        stubFor(get(up1)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(authorization))
                .withHeader(HttpHeaders.COOKIE, equalTo(cookie))
                .withHeader("Cookie2", equalTo(cookie2))
                .withHeader(HttpHeaders.PROXY_AUTHORIZATION, equalTo(proxyAuthorization))
                .withHeader(HttpHeaders.WWW_AUTHENTICATE, equalTo(wwwAuthenticate))
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", redirectWireMock2.url("elvis"))));

        // headers should be absent after we've been redirected
        UrlPattern up2 = urlPathEqualTo("/elvis");
        redirectWireMock2.stubFor(get(up2)
                .withHeader(HttpHeaders.AUTHORIZATION, absent())
                .withHeader(HttpHeaders.COOKIE, absent())
                .withHeader("Cookie2", absent())
                .withHeader(HttpHeaders.PROXY_AUTHORIZATION, absent())
                .withHeader(HttpHeaders.WWW_AUTHENTICATE, absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", redirectWireMock2.url(TEST_FILE_NAME2))));

        UrlPattern up3 = urlPathEqualTo("/" + TEST_FILE_NAME2);
        redirectWireMock2.stubFor(get(up3)
                .withHeader(HttpHeaders.AUTHORIZATION, absent())
                .withHeader(HttpHeaders.COOKIE, absent())
                .withHeader("Cookie2", absent())
                .withHeader(HttpHeaders.PROXY_AUTHORIZATION, absent())
                .withHeader(HttpHeaders.WWW_AUTHENTICATE, absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", wireMock.url("final"))));

        // headers should be back again
        UrlPattern up4 = urlPathEqualTo("/final");
        stubFor(get(up4)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(authorization))
                .withHeader(HttpHeaders.COOKIE, equalTo(cookie))
                .withHeader("Cookie2", equalTo(cookie2))
                .withHeader(HttpHeaders.PROXY_AUTHORIZATION, equalTo(proxyAuthorization))
                .withHeader(HttpHeaders.WWW_AUTHENTICATE, equalTo(wwwAuthenticate))
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", wireMock.url(TEST_FILE_NAME))));

        Download t = makeProjectAndTask();
        t.src(wireMock.url("auth"));
        t.header(HttpHeaders.AUTHORIZATION, authorization);
        t.header(HttpHeaders.COOKIE, cookie);
        t.header("Cookie2", cookie2);
        t.header(HttpHeaders.PROXY_AUTHORIZATION, proxyAuthorization);
        t.header(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        verify(1, getRequestedFor(up1));
        redirectWireMock2.verify(1, getRequestedFor(up2));
        redirectWireMock2.verify(1, getRequestedFor(up3));
        verify(1, getRequestedFor(up4));
    }

    /**
     * Test if basic auth headers are removed from the request after redirecting
     * to another host
     * @throws Exception if anything goes wrong
     */
    @Test
    public void removeBasicAuth() throws Exception {
        configureDefaultStub();

        String realm = "Gradle";
        String username = "testuser123";
        String password = "testpass456";

        String ahdr = "Basic " + Base64.encodeBase64String(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        UrlPattern up1 = urlPathEqualTo("/auth");
        stubFor(get(up1)
                .willReturn(aResponse()
                        .withHeader("WWW-Authenticate",
                                "Basic realm=\"" + realm + "\"")
                        .withStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        stubFor(get(up1)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(ahdr))
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", redirectWireMock2.url("elvis"))));

        UrlPattern up2 = urlPathEqualTo("/elvis");
        redirectWireMock2.stubFor(get(up2)
                .withHeader(HttpHeaders.AUTHORIZATION, absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", wireMock.url(TEST_FILE_NAME))));

        Download t = makeProjectAndTask();
        t.src(wireMock.url("auth"));
        t.username(username);
        t.password(password);
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        verify(2, getRequestedFor(up1));
        redirectWireMock2.verify(1, getRequestedFor(up2));
    }

    /**
     * Test if basic auth headers are removed from the request after redirecting
     * to another host
     * @throws Exception if anything goes wrong
     */
    @Test
    public void removeBasicAuthPreauth() throws Exception {
        configureDefaultStub();

        String username = "testuser123";
        String password = "testpass456";

        String ahdr = "Basic " + Base64.encodeBase64String(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        UrlPattern up1 = urlPathEqualTo("/auth");
        stubFor(get(up1)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(ahdr))
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", redirectWireMock2.url("elvis"))));

        UrlPattern up2 = urlPathEqualTo("/elvis");
        redirectWireMock2.stubFor(get(up2)
                .withHeader(HttpHeaders.AUTHORIZATION, absent())
                .willReturn(aResponse()
                        .withStatus(HttpServletResponse.SC_FOUND)
                        .withHeader("Location", wireMock.url(TEST_FILE_NAME))));

        Download t = makeProjectAndTask();
        t.src(wireMock.url("auth"));
        t.username(username);
        t.password(password);
        t.preemptiveAuth(true);
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);

        verify(1, getRequestedFor(up1));
        redirectWireMock2.verify(1, getRequestedFor(up2));
    }
}

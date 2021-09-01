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

//import com.github.tomakehurst.wiremock.common.FileSource;
//import com.github.tomakehurst.wiremock.extension.Parameters;
//import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
//import com.github.tomakehurst.wiremock.http.HttpHeader;
//import com.github.tomakehurst.wiremock.http.Request;
//import com.github.tomakehurst.wiremock.http.Response;
//import com.github.tomakehurst.wiremock.junit.WireMockRule;
//import com.github.tomakehurst.wiremock.matching.UrlPattern;
//import org.apache.commons.io.FileUtils;
//import org.gradle.api.tasks.TaskExecutionException;
//import org.junit.Rule;
//import org.junit.Test;
//
//import javax.servlet.http.HttpServletResponse;
//import java.io.File;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.matching;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
//import static com.github.tomakehurst.wiremock.client.WireMock.verify;
//import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
//import static org.junit.Assert.assertEquals;
//
///**
// * Tests if the plugin can handle redirects
// * @author Michel Kraemer
// */
//public class RedirectTest extends TestBaseWithMockServer {
//    private static final String REDIRECT = "redirect";
//
//    /**
//     * Run a mock HTTP server with {@link RedirectTransformer}
//     */
//    @Rule
//    public WireMockRule redirectWireMockRule = new WireMockRule(options()
//            .dynamicPort()
//            .extensions(RedirectTransformer.class.getName())
//            .jettyStopTimeout(10000L));
//
//    public static class RedirectTransformer extends ResponseTransformer {
//        private Integer redirects = null;
//
//        @Override
//        public Response transform(Request request, Response response,
//                FileSource files, Parameters parameters) {
//            if (redirects == null) {
//                redirects = parameters.getInt("redirects");
//            }
//            String nl;
//            if (redirects > 1) {
//                redirects--;
//                nl = "/" + REDIRECT + "?r=" + redirects;
//            } else {
//                nl = "/" + TEST_FILE_NAME;
//            }
//            return Response.Builder.like(response)
//                    .but().headers(response.getHeaders()
//                            .plus(new HttpHeader("Location", nl)))
//                    .build();
//        }
//
//        @Override
//        public String getName() {
//            return "redirect";
//        }
//    }
//
//    /**
//     * Tests if the plugin can handle one redirect
//     * @throws Exception if anything goes wrong
//     */
//    @Test
//    public void oneRedirect() throws Exception {
//        UrlPattern up1 = urlEqualTo("/" + REDIRECT);
//        wireMockRule.stubFor(get(up1)
//                .willReturn(aResponse()
//                        .withStatus(HttpServletResponse.SC_FOUND)
//                        .withHeader("Location", wireMockRule.url(TEST_FILE_NAME))));
//
//        UrlPattern up2 = urlEqualTo("/" + TEST_FILE_NAME);
//        wireMockRule.stubFor(get(up2)
//                .willReturn(aResponse()
//                        .withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.src(wireMockRule.url(REDIRECT));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//
//        wireMockRule.verify(1, getRequestedFor(up1));
//        wireMockRule.verify(1, getRequestedFor(up2));
//    }
//
//    /**
//     * Tests if the plugin can handle ten redirects
//     * @throws Exception if anything goes wrong
//     */
//    @Test
//    public void tenRedirect() throws Exception {
//        UrlPattern up1 = urlPathEqualTo("/" + REDIRECT);
//        redirectWireMockRule.stubFor(get(up1)
//                .withQueryParam("r", matching("[0-9]+"))
//                .willReturn(aResponse()
//                        .withStatus(HttpServletResponse.SC_FOUND)
//                        .withTransformer("redirect", "redirects", 10)));
//
//        UrlPattern up2 = urlEqualTo("/" + TEST_FILE_NAME);
//        redirectWireMockRule.stubFor(get(up2)
//                .willReturn(aResponse()
//                        .withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.src(redirectWireMockRule.url(REDIRECT) + "?r=10");
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//
//        verify(10, getRequestedFor(up1));
//        verify(1, getRequestedFor(up2));
//    }
//
//   /**
//    * Tests if the plugin can handle circular redirects
//    * @throws Exception if anything goes wrong
//    */
//   @Test(expected = TaskExecutionException.class)
//   public void circularRedirect() throws Exception {
//       UrlPattern up1 = urlPathEqualTo("/" + REDIRECT);
//       wireMockRule.stubFor(get(up1)
//               .willReturn(aResponse()
//                       .withStatus(HttpServletResponse.SC_FOUND)
//                       .withHeader("Location", "/" + REDIRECT)));
//
//       Download t = makeProjectAndTask();
//       t.src(wireMockRule.url(REDIRECT));
//       File dst = folder.newFile();
//       t.dest(dst);
//       t.execute();
//   }
//
//    /**
//     * Make sure the plugin fails with too many redirects
//     * @throws Exception if anything goes wrong
//     */
//    @Test(expected = TaskExecutionException.class)
//    public void tooManyRedirects() throws Exception {
//        UrlPattern up1 = urlPathEqualTo("/" + REDIRECT);
//        redirectWireMockRule.stubFor(get(up1)
//                .withQueryParam("r", matching("[0-9]+"))
//                .willReturn(aResponse()
//                        .withStatus(HttpServletResponse.SC_FOUND)
//                        .withTransformer("redirect", "redirects", 51)));
//
//        Download t = makeProjectAndTask();
//        t.src(redirectWireMockRule.url(REDIRECT) + "?r=52");
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//    }
//}

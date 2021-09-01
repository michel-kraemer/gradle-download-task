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

//import org.apache.commons.io.FileUtils;
//import org.junit.Test;
//
//import java.io.File;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.absent;
//import static com.github.tomakehurst.wiremock.client.WireMock.containing;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//import static org.junit.Assert.assertEquals;
//
///**
// * Tests if the plugin can handle compressed content
// * @author Michel Kraemer
// */
//public class CompressionTest extends TestBaseWithMockServer {
//    /**
//     * Tests if the plugin can handle compressed content
//     * @throws Exception if anything goes wrong
//     */
//    @Test
//    public void compressed() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .withHeader("accept-encoding", containing("gzip"))
//                .willReturn(aResponse()
//                        .withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//    }
//
//    /**
//     * Tests if the plugin can request uncompressed content
//     * @throws Exception if anything goes wrong
//     */
//    @Test
//    public void uncompressed() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .withHeader("accept-encoding", absent())
//                .willReturn(aResponse()
//                        .withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.compress(false);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//    }
//}

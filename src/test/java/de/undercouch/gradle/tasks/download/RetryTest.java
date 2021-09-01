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

//import com.github.tomakehurst.wiremock.http.Fault;
//import de.undercouch.gradle.tasks.download.org.apache.http.NoHttpResponseException;
//import org.apache.commons.io.FileUtils;
//import org.gradle.api.UncheckedIOException;
//import org.gradle.api.tasks.TaskExecutionException;
//import org.junit.Test;
//
//import java.io.File;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//
///**
// * Tests if the a download can be retried
// * @author Michel Kraemer
// */
//public class RetryTest extends TestBaseWithMockServer {
//    private static final String SCENARIO = "scenario";
//    private static final String TWO = "two";
//    private static final String THREE = "three";
//    private static final String FOUR = "four";
//
//    /**
//     * Test if the download task does not retry requests by default
//     * @throws Exception if anything else goes wrong
//     */
//    @Test
//    public void retryDefault() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(STARTED)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(TWO));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(TWO)
//                .willReturn(aResponse().withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        assertEquals(0, t.getRetries());
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        try {
//            t.execute();
//            fail("Request should have failed");
//        } catch (TaskExecutionException e) {
//            wireMockRule.verify(1, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
//            assertTrue(e.getCause() instanceof UncheckedIOException);
//            assertTrue(e.getCause().getCause() instanceof NoHttpResponseException);
//        }
//    }
//
//    /**
//     * Test if the download task can retry once
//     * @throws Exception if anything else goes wrong
//     */
//    @Test
//    public void retryOnce() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(STARTED)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(TWO));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(TWO)
//                .willReturn(aResponse().withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.retries(1);
//        assertEquals(1, t.getRetries());
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//
//        wireMockRule.verify(2, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
//    }
//
//    /**
//     * Test if the download task can retry three times
//     * @throws Exception if anything else goes wrong
//     */
//    @Test
//    public void retryThree() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(STARTED)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(TWO));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(TWO)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(THREE));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(THREE)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(FOUR));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(FOUR)
//                .willReturn(aResponse().withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.retries(3);
//        assertEquals(3, t.getRetries());
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//
//        wireMockRule.verify(4, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
//    }
//
//    /**
//     * Test if the download task can retry up to three times
//     * @throws Exception if anything else goes wrong
//     */
//    @Test
//    public void retryUpToThree() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(STARTED)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(TWO));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(TWO)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(THREE));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(THREE)
//                .willReturn(aResponse().withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.retries(3);
//        assertEquals(3, t.getRetries());
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//
//        wireMockRule.verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
//    }
//
//    /**
//     * Test if the download task can retry two times and the fails
//     * @throws Exception if anything else goes wrong
//     */
//    @Test
//    public void retryTwo() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(STARTED)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(TWO));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(TWO)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(THREE));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(THREE)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(FOUR));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(FOUR)
//                .willReturn(aResponse().withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.retries(2);
//        assertEquals(2, t.getRetries());
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        try {
//            t.execute();
//            fail("Request should have failed");
//        } catch (TaskExecutionException e) {
//            wireMockRule.verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
//            assertTrue(e.getCause() instanceof UncheckedIOException);
//            assertTrue(e.getCause().getCause() instanceof NoHttpResponseException);
//        }
//    }
//
//    /**
//     * Test if the download task can retry infinitely
//     * @throws Exception if anything else goes wrong
//     */
//    @Test
//    public void inifinite() throws Exception {
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(STARTED)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(TWO));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(TWO)
//                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
//                .willSetStateTo(THREE));
//
//        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
//                .inScenario(SCENARIO)
//                .whenScenarioStateIs(THREE)
//                .willReturn(aResponse().withBody(CONTENTS)));
//
//        Download t = makeProjectAndTask();
//        t.retries(-1);
//        assertEquals(-1, t.getRetries());
//        t.src(wireMockRule.url(TEST_FILE_NAME));
//        File dst = folder.newFile();
//        t.dest(dst);
//        t.execute();
//
//        String dstContents = FileUtils.readFileToString(dst);
//        assertEquals(CONTENTS, dstContents);
//
//        wireMockRule.verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
//    }
//}

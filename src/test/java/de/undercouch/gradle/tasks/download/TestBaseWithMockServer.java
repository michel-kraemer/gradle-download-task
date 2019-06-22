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

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Rule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Base class for unit tests that require a mock HTTP server
 * @author Michel Kraemer
 */
public class TestBaseWithMockServer extends TestBase {
    /**
     * Run a mock HTTP server
     */
    @ClassRule
    public static WireMockClassRule classWireMockRule = new WireMockClassRule(options()
            .dynamicPort()
            .jettyStopTimeout(10000L));

    @Rule
    public WireMockClassRule wireMockRule = classWireMockRule;

    protected void configureDefaultStub() {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));
    }

    protected void configureDefaultStub2() {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME2))
                .willReturn(aResponse()
                        .withBody(CONTENTS2)));
    }
}

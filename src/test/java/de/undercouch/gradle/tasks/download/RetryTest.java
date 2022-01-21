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

import com.github.tomakehurst.wiremock.http.Fault;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

/**
 * Tests if a download can be retried
 * @author Michel Kraemer
 */
public class RetryTest extends TestBaseWithMockServer {
    private static final String SCENARIO = "scenario";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";

    /**
     * Test if the download task does not retry requests by default
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void retryDefault() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        assertThat(t.getRetries()).isZero();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .getRootCause()
                .isInstanceOf(NoHttpResponseException.class)
                .hasMessageContaining("failed to respond");
        verify(1, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
    }

    /**
     * Test if the download task can retry once
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void retryOnce() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.retries(1);
        assertThat(t.getRetries()).isOne();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        verify(2, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
    }

    /**
     * Test if the download task can retry three times
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void retryThree() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(THREE));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(THREE)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(FOUR));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(FOUR)
                .willReturn(aResponse().withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.retries(3);
        assertThat(t.getRetries()).isEqualTo(3);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        verify(4, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
    }

    /**
     * Test if the download task can retry up to three times
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void retryUpToThree() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(THREE));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(THREE)
                .willReturn(aResponse().withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.retries(3);
        assertThat(t.getRetries()).isEqualTo(3);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
    }

    /**
     * Test if the download task can retry two times and then fails
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void retryTwo() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(THREE));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(THREE)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(FOUR));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(FOUR)
                .willReturn(aResponse().withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.retries(2);
        assertThat(t.getRetries()).isEqualTo(2);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .getRootCause()
                .isInstanceOf(NoHttpResponseException.class)
                .hasMessageContaining("failed to respond");
        verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
    }

    /**
     * Test if the download task can retry infinitely
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void inifinite() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(THREE));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(THREE)
                .willReturn(aResponse().withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        t.retries(-1);
        assertThat(t.getRetries()).isEqualTo(-1);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));
    }

    /**
     * Test if the download task logs retry attempts
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void logRetries() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(THREE));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(THREE)
                .willReturn(aResponse().withBody(CONTENTS)));

        Project project = makeProject();

        // spy on the logger and record all messages
        List<String> recordedWarn = new ArrayList<>();
        List<String> recordedDebug = new ArrayList<>();
        List<Throwable> recordedExceptions = new ArrayList<>();
        Logger realLogger = project.getLogger();
        Logger logger = spy(realLogger);
        doAnswer(msg -> {
            recordedWarn.add(msg.getArgument(0));
            realLogger.info(msg.getArgument(0));
            return null;
        }).when(logger).warn(anyString());
        doAnswer(msg -> {
            recordedDebug.add(msg.getArgument(0));
            realLogger.info(msg.getArgument(0));
            recordedExceptions.add(msg.getArgument(1));
            return null;
        }).when(logger).debug(anyString(), (Throwable)any());
        Download t = makeTask(project);

        // replace logger in DownloadAction
        Field actionField = Download.class.getDeclaredField("action");
        actionField.setAccessible(true);
        DownloadAction action = (DownloadAction)actionField.get(t);
        Field loggerField = DownloadAction.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(action, logger);

        t.retries(2);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        verify(3, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));

        assertThat(recordedWarn)
                .containsExactly("Request attempt 1/2 failed. Retrying ...",
                        "Request attempt 2/2 failed. Retrying ...");
        assertThat(recordedDebug).containsExactly("Request attempt failed",
                "Request attempt failed");
        assertThat(recordedExceptions).hasSize(2);
        assertThat(recordedExceptions.get(0))
                .isInstanceOf(NoHttpResponseException.class);
        assertThat(recordedExceptions.get(1))
                .isInstanceOf(NoHttpResponseException.class);
    }

    /**
     * Test if the download task does not log retry attempts if the quiet flag is set
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void quietLogRetries() throws Exception {
        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(TWO));

        stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(TWO)
                .willReturn(aResponse().withBody(CONTENTS)));

        Project project = makeProject();

        // spy on the logger
        Logger realLogger = project.getLogger();
        Logger logger = spy(realLogger);
        Download t = makeTask(project);

        // replace logger in DownloadAction
        Field actionField = Download.class.getDeclaredField("action");
        actionField.setAccessible(true);
        DownloadAction action = (DownloadAction)actionField.get(t);
        Field loggerField = DownloadAction.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(action, logger);

        t.retries(1);
        t.quiet(true);
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
        verify(2, getRequestedFor(urlEqualTo("/" + TEST_FILE_NAME)));

        Mockito.verify(logger, never()).warn(anyString());
    }
}

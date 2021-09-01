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
package de.undercouch.gradle.tasks.download

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
//import org.junit.Before
//import org.junit.Rule
//import org.junit.rules.TemporaryFolder
//import org.junit.ClassRule
//import org.junit.Rule
//import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest

/**
 * Base class for unit tests
 * @author Michel Kraemer
 */
abstract class TestBase {

    /** A temporary directory where a virtual test project is stored */
    private var projectDir: File? = null

    /**
     * A folder for temporary files
     */
    @TempDir
    lateinit var folder: File

    /**
     * Set up the unit tests
     * @throws Exception if anything goes wrong
     */
    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
//        projectDir = folder.newFolder("project")
    }

    /**
     * Makes a Gradle project and creates a download task
     * @return the unconfigured download task
     */
    protected fun makeProjectAndTask(projectConfiguration: Action<Project?>? = null): Download {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        projectConfiguration?.execute(project)
//        val applyParams: MutableMap<String, Any?> = hashMapOf("plugin" to "de.undercouch.download")
//        project.apply(applyParams)
        project.plugins.apply("de.undercouch.download")
        val taskParams: MutableMap<String, Any?> = hashMapOf("type" to Download::class.java)
        return project.task(taskParams, "downloadFile") as Download
    }

    companion object {
        const val TEST_FILE_NAME = "test.txt"
        const val CONTENTS = "Hello world"
        const val TEST_FILE_NAME2 = "test2.txt"
        const val CONTENTS2 = "Elvis lives!"
    }
}


/**
 * Base class for unit tests that require a mock HTTP server
 * @author Michel Kraemer
 */
abstract class TestBaseWithMockServer : TestBase() {

//    @Rule
    var wireMockRule = classWireMockRule
    protected fun configureDefaultStub() {
        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/$TEST_FILE_NAME"))
                                 .willReturn(WireMock.aResponse().withBody(CONTENTS)))
    }

    protected fun configureDefaultStub2() {
        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/$TEST_FILE_NAME2"))
                                 .willReturn(WireMock.aResponse().withBody(CONTENTS2)))
    }

    companion object {
        /** Run a mock HTTP server */
//        @ClassRule
        var classWireMockRule = WireMockClassRule(WireMockConfiguration.options()
                                                      .dynamicPort()
                                                      .jettyStopTimeout(10000L))
    }
}
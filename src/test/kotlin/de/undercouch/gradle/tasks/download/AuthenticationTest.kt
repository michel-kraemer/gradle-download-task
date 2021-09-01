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

//import de.undercouch.gradle.tasks.download.Download.authScheme
import com.github.tomakehurst.wiremock.client.WireMock
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
//import org.junit.Assert
import java.io.File
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletResponse
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests if the plugin can access a resource that requires authentication
 * @author Michel Kraemer
 */
class MagikPluginTest {
    @Test fun a() {
        // Create a test project and apply the plugin
//        val project = ProjectBuilder.builder().build()
//        project.plugins.apply("magik.greeting")
//
//        // Verify the result
//        assertNotNull(project.tasks.findByName("greeting"))
    }
}
class AuthenticationTest : TestBaseWithMockServer() {

    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test//(expected = TaskExecutionException::class)
//    @Throws(Exception::class)
    fun noAuthorization() {
//        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/$AUTHENTICATE"))
//                                 .withHeader("Authorization", WireMock.absent())
//                                 .willReturn(WireMock.aResponse()
//                                                 .withStatus(HttpServletResponse.SC_UNAUTHORIZED)))
//        val t: Download = makeProjectAndTask()
//        t.src = wireMockRule.url(AUTHENTICATE)
//        t.dest = folder.newFile()
//        t.execute()
    }

    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
//    @Test(expected = TaskExecutionException::class) @Throws(Exception::class) fun invalidCredentials() {
//        val wrongUser = USERNAME + "!"
//        val wrongPass = PASSWORD + "!"
//        val ahdr = "Basic " + Base64.encodeBase64String(
//            "$wrongUser:$wrongPass".toByteArray(StandardCharsets.UTF_8))
//        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/" + AUTHENTICATE))
//                                 .withHeader("Authorization", WireMock.equalTo(ahdr))
//                                 .willReturn(WireMock.aResponse()
//                                                 .withStatus(HttpServletResponse.SC_UNAUTHORIZED)))
//        val t: Download = makeProjectAndTask()
//        t.src(wireMockRule.url(AUTHENTICATE))
//        val dst: File = folder.newFile()
//        t.dest(dst)
//        t.username(wrongUser)
//        t.password(wrongPass)
//        t.execute()
//    }
//
//    /**
//     * Tests if the plugin can access a protected resource
//     * @throws Exception if anything goes wrong
//     */
//    @Test @Throws(Exception::class) fun validUserAndPass() {
//        val ahdr = "Basic " + Base64.encodeBase64String(
//            (USERNAME + ":" + PASSWORD).toByteArray(StandardCharsets.UTF_8))
//        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/" + AUTHENTICATE))
//                                 .withHeader("Authorization", WireMock.equalTo(ahdr))
//                                 .willReturn(WireMock.aResponse()
//                                                 .withBody(CONTENTS)))
//        val t: Download = makeProjectAndTask()
//        t.src(wireMockRule.url(AUTHENTICATE))
//        val dst: File = folder.newFile()
//        t.dest(dst)
//        t.username(USERNAME)
//        t.password(PASSWORD)
//        t.execute()
//        val dstContents = FileUtils.readFileToString(dst)
//        assertEquals(CONTENTS, dstContents)
//    }
//
//    /**
//     * Tests if the plugin can access a protected resource
//     * @throws Exception if anything goes wrong
//     */
//    @Test @Throws(Exception::class) fun validDigest() {
//        val ha1 = DigestUtils.md5Hex(
//            USERNAME + ":" + REALM + ":" + PASSWORD)
//        val ha2 = DigestUtils.md5Hex(
//            "GET:/" + AUTHENTICATE)
//        val expectedResponse = DigestUtils.md5Hex(
//            ha1 + ":" + NONCE + ":" + ha2)
//        val ahdr = "Digest username=\"" + USERNAME + "\", " +
//                "realm=\"" + REALM + "\", " +
//                "nonce=\"" + NONCE + "\", " +
//                "uri=\"/" + AUTHENTICATE + "\", " +
//                "response=\"" + expectedResponse + "\", " +
//                "algorithm=MD5"
//        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/" + AUTHENTICATE))
//                                 .withHeader("Authorization", WireMock.absent())
//                                 .willReturn(WireMock.aResponse()
//                                                 .withHeader("WWW-Authenticate",
//                                                             "Digest realm=\"" + REALM + "\"," +
//                                                                     "nonce=\"" + NONCE + "\"")
//                                                 .withStatus(HttpServletResponse.SC_UNAUTHORIZED)))
//        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/" + AUTHENTICATE))
//                                 .withHeader("Authorization", WireMock.equalTo(ahdr))
//                                 .willReturn(WireMock.aResponse()
//                                                 .withBody(CONTENTS)))
//        val t: Download = makeProjectAndTask()
//        t.src(wireMockRule.url(AUTHENTICATE))
//        val dst: File = folder.newFile()
//        t.dest(dst)
//        t.username(USERNAME)
//        t.password(PASSWORD)
//        t.authScheme("Digest")
//        t.execute()
//    }
//
//    /**
//     * Make sure the plugin rejects an invalid authentication scheme
//     * @throws Exception if anything goes wrong
//     */
//    @Test(expected = IllegalArgumentException::class) @Throws(Exception::class) fun invalidAuthSchemeString() {
//        val t: Download = makeProjectAndTask()
//        t.src(wireMockRule.url(AUTHENTICATE))
//        val dst: File = folder.newFile()
//        t.dest(dst)
//        t.authScheme("Foobar")
//        t.execute()
//    }
//
//    /**
//     * Tests if the plugin has no authentications scheme set by default
//     */
//    @Test fun noDefaultAuthScheme() {
//        val t: Download = makeProjectAndTask()
//        Assert.assertNull(t.authScheme)
//    }

    companion object {
        private const val PASSWORD = "testpass456"
        private const val USERNAME = "testuser123"
        private const val AUTHENTICATE = "authenticate"
        private const val REALM = "Gradle"
        private const val NONCE = "ABCDEF0123456789"
    }
}
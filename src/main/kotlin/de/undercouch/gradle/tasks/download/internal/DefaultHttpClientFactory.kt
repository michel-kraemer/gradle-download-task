// Copyright 2016-2019 Michel Kraemer
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
package de.undercouch.gradle.tasks.download.internal

import org.apache.commons.logging.impl.NoOpLog
import org.gradle.internal.impldep.org.apache.http.HttpHost
import org.gradle.internal.impldep.org.apache.http.config.Registry
import org.gradle.internal.impldep.org.apache.http.config.RegistryBuilder
import org.gradle.internal.impldep.org.apache.http.conn.HttpClientConnectionManager
import org.gradle.internal.impldep.org.apache.http.conn.socket.ConnectionSocketFactory
import org.gradle.internal.impldep.org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.gradle.internal.impldep.org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.gradle.internal.impldep.org.apache.http.impl.client.CloseableHttpClient
import org.gradle.internal.impldep.org.apache.http.impl.client.HttpClientBuilder
import org.gradle.internal.impldep.org.apache.http.impl.conn.BasicHttpClientConnectionManager
import org.gradle.internal.impldep.org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * Default implementation of [HttpClientFactory]. Creates a new client
 * every time [.createHttpClient]
 * is called. The caller is responsible for closing this client.
 * @author Michel Kraemer
 */
open class DefaultHttpClientFactory : HttpClientFactory {
    private var insecureSSLSocketFactory: SSLConnectionSocketFactory? = null
        get() {
            if (field == null) {
                val sc: SSLContext
                try {
                    sc = SSLContext.getInstance("SSL")
                    sc.init(null, INSECURE_TRUST_MANAGERS, SecureRandom())
                    field = SSLConnectionSocketFactory(sc, INSECURE_HOSTNAME_VERIFIER)
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                } catch (e: KeyManagementException) {
                    throw RuntimeException(e)
                }
            }
            return field
        }

    override fun createHttpClient(httpHost: HttpHost, acceptAnyCertificate: Boolean, retries: Int): CloseableHttpClient {
        //disable logging by default to improve download performance
        //see issue 141 (https://github.com/michel-kraemer/gradle-download-task/issues/141)
        if (System.getProperty(LOG_PROPERTY) == null)
            System.setProperty(LOG_PROPERTY, NoOpLog::class.java.name)
        val builder: HttpClientBuilder = HttpClientBuilder.create()

        //configure retries
        if (retries == 0)
            builder.disableAutomaticRetries()
        else
            builder.setRetryHandler { _, i, _ -> retries < 0 || i <= retries }

        //configure proxy from system environment
        builder.setRoutePlanner(SystemDefaultRoutePlanner(null))

        //accept any certificate if necessary
        if ("https" == httpHost.schemeName && acceptAnyCertificate) {
            val icsf: SSLConnectionSocketFactory? = insecureSSLSocketFactory
            builder.setSSLSocketFactory(icsf)
            val registry: Registry<ConnectionSocketFactory> = RegistryBuilder.create<ConnectionSocketFactory?>()
                .register("https", icsf)
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .build()
            val cm: HttpClientConnectionManager = BasicHttpClientConnectionManager(registry)
            builder.setConnectionManager(cm)
        }
        return builder.build()
    }

    companion object {
        private const val LOG_PROPERTY = "de.undercouch.gradle.tasks.download.org.apache.commons.logging.Log"
        private val INSECURE_HOSTNAME_VERIFIER: HostnameVerifier = InsecureHostnameVerifier()
        private val INSECURE_TRUST_MANAGERS = arrayOf<TrustManager>(InsecureTrustManager())
    }
}
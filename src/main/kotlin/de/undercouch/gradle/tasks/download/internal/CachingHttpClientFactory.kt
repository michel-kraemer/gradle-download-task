// Copyright 2016 Michel Kraemer
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

import org.gradle.internal.impldep.org.apache.http.HttpHost
import org.gradle.internal.impldep.org.apache.http.impl.client.CloseableHttpClient
import java.io.IOException
import java.util.*

/**
 * An implementation of [HttpClientFactory] that caches created clients
 * until the [.close] method is called.
 * @author Michel Kraemer
 */
class CachingHttpClientFactory : DefaultHttpClientFactory() {
    private val cachedClients: MutableMap<CacheKey, CloseableHttpClient> = HashMap()
    override fun createHttpClient(httpHost: HttpHost, acceptAnyCertificate: Boolean, retries: Int): CloseableHttpClient {
        val key = CacheKey(httpHost, acceptAnyCertificate, retries)
        return cachedClients.getOrPut(key) { super.createHttpClient(httpHost, acceptAnyCertificate, retries) }
    }

    /**
     * Close all HTTP clients created by this factory
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) fun close() {
        for (c in cachedClients.values)
            c.close()
        cachedClients.clear()
    }

    /**
     * A key in the map of cached HTTP clients
     */
    private class CacheKey(private val httpHost: HttpHost, private val acceptAnyCertificate: Boolean, private val retries: Int) {
        override fun equals(other: Any?): Boolean = when {
            this === other -> true
            other == null || javaClass != other.javaClass -> false
            else -> {
                val cacheKey = other as CacheKey
                acceptAnyCertificate == cacheKey.acceptAnyCertificate && retries == cacheKey.retries && httpHost == cacheKey.httpHost
            }
        }

        override fun hashCode(): Int = Objects.hash(httpHost, acceptAnyCertificate, retries)
    }
}
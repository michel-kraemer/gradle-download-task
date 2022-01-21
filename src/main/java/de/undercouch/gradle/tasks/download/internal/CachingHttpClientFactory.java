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

package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpHost;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of {@link HttpClientFactory} that caches created clients
 * until the {@link #close()} method is called.
 * @author Michel Kraemer
 */
public class CachingHttpClientFactory extends DefaultHttpClientFactory {
    private final Map<CacheKey, CloseableHttpClient> cachedClients = new HashMap<>();

    @Override
    public CloseableHttpClient createHttpClient(HttpHost httpHost,
            boolean acceptAnyCertificate, int retries, Logger logger, boolean quiet) {
        CacheKey key = new CacheKey(httpHost, acceptAnyCertificate, retries);
        CloseableHttpClient c = cachedClients.get(key);
        if (c == null) {
            c = super.createHttpClient(httpHost, acceptAnyCertificate, retries,
                    logger, quiet);
            cachedClients.put(key, c);
        }
        return c;
    }

    /**
     * Close all HTTP clients created by this factory
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        for (CloseableHttpClient c : cachedClients.values()) {
            c.close();
        }
        cachedClients.clear();
    }
    
    /**
     * A key in the map of cached HTTP clients
     */
    private static class CacheKey {
        private final HttpHost httpHost;
        private final boolean acceptAnyCertificate;
        private final int retries;

        CacheKey(HttpHost httpHost, boolean acceptAnyCertificate, int retries) {
            this.httpHost = httpHost;
            this.acceptAnyCertificate = acceptAnyCertificate;
            this.retries = retries;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey)o;
            return acceptAnyCertificate == cacheKey.acceptAnyCertificate &&
                    retries == cacheKey.retries &&
                    httpHost.equals(cacheKey.httpHost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(httpHost, acceptAnyCertificate, retries);
        }
    }
}

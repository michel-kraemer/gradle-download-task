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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * An implementation of {@link HttpClientFactory} that caches created clients
 * until the {@link #close()} method is called.
 * @author Michel Kraemer
 */
public class CachingHttpClientFactory extends DefaultHttpClientFactory {
    private Map<HttpHost, CloseableHttpClient> cachedAcceptingClients =
            new HashMap<HttpHost, CloseableHttpClient>();
    private Map<HttpHost, CloseableHttpClient> cachedClients =
            new HashMap<HttpHost, CloseableHttpClient>();

    @Override
    public CloseableHttpClient createHttpClient(HttpHost httpHost,
            boolean acceptAnyCertificate) {
        CloseableHttpClient c;
        if (acceptAnyCertificate) {
            c = cachedAcceptingClients.get(httpHost);
        } else {
            c = cachedClients.get(httpHost);
        }
        if (c == null) {
            c = super.createHttpClient(httpHost, acceptAnyCertificate);
            if (acceptAnyCertificate) {
                cachedAcceptingClients.put(httpHost, c);
            } else {
                cachedClients.put(httpHost, c);
            }
        }
        return c;
    }

    /**
     * Close all HTTP clients created by this factory
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        for (CloseableHttpClient c : cachedAcceptingClients.values()) {
            c.close();
        }
        cachedAcceptingClients.clear();
        for (CloseableHttpClient c : cachedClients.values()) {
            c.close();
        }
        cachedClients.clear();
    }
}

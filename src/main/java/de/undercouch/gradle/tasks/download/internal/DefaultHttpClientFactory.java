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

package de.undercouch.gradle.tasks.download.internal;

import de.undercouch.gradle.tasks.download.org.apache.http.HttpHost;
import de.undercouch.gradle.tasks.download.org.apache.http.client.HttpRequestRetryHandler;
import de.undercouch.gradle.tasks.download.org.apache.http.config.Registry;
import de.undercouch.gradle.tasks.download.org.apache.http.config.RegistryBuilder;
import de.undercouch.gradle.tasks.download.org.apache.http.conn.HttpClientConnectionManager;
import de.undercouch.gradle.tasks.download.org.apache.http.conn.socket.ConnectionSocketFactory;
import de.undercouch.gradle.tasks.download.org.apache.http.conn.socket.PlainConnectionSocketFactory;
import de.undercouch.gradle.tasks.download.org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.client.CloseableHttpClient;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.client.HttpClientBuilder;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import de.undercouch.gradle.tasks.download.org.apache.http.protocol.HttpContext;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Default implementation of {@link HttpClientFactory}. Creates a new client
 * every time {@link #createHttpClient(HttpHost, boolean, int)}
 * is called. The caller is responsible for closing this client.
 * @author Michel Kraemer
 */
public class DefaultHttpClientFactory implements HttpClientFactory {
    private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER =
            new InsecureHostnameVerifier();
    private static final TrustManager[] INSECURE_TRUST_MANAGERS =
        { new InsecureTrustManager() };

    private SSLConnectionSocketFactory insecureSSLSocketFactory = null;

    @Override
    public CloseableHttpClient createHttpClient(HttpHost httpHost,
            boolean acceptAnyCertificate, final int retries) {
        HttpClientBuilder builder = HttpClientBuilder.create();

        //configure retries
        if (retries == 0) {
            builder.disableAutomaticRetries();
        } else {
            builder.setRetryHandler(new HttpRequestRetryHandler() {
                @Override
                public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                    return retries < 0 || i <= retries;
                }
            });
        }

        //configure proxy from system environment
        builder.setRoutePlanner(new SystemDefaultRoutePlanner(null));
        
        //accept any certificate if necessary
        if ("https".equals(httpHost.getSchemeName()) && acceptAnyCertificate) {
            SSLConnectionSocketFactory icsf = getInsecureSSLSocketFactory();
            builder.setSSLSocketFactory(icsf);
            Registry<ConnectionSocketFactory> registry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", icsf)
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .build();
            HttpClientConnectionManager cm =
                    new BasicHttpClientConnectionManager(registry);
            builder.setConnectionManager(cm);
        }
        
        return builder.build();
    }
    
    private SSLConnectionSocketFactory getInsecureSSLSocketFactory() {
        if (insecureSSLSocketFactory == null) {
            SSLContext sc;
            try {
                sc = SSLContext.getInstance("SSL");
                sc.init(null, INSECURE_TRUST_MANAGERS, new SecureRandom());
                insecureSSLSocketFactory = new SSLConnectionSocketFactory(
                        sc, INSECURE_HOSTNAME_VERIFIER);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
        return insecureSSLSocketFactory;
    }
}

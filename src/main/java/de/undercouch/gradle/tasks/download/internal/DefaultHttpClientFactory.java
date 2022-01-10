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

import org.apache.commons.logging.impl.NoOpLog;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.TimeValue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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
    private static String LOG_PROPERTY =
            "de.undercouch.gradle.tasks.download.org.apache.commons.logging.Log";

    private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER =
            new InsecureHostnameVerifier();
    private static final TrustManager[] INSECURE_TRUST_MANAGERS =
        { new InsecureTrustManager() };

    private SSLConnectionSocketFactory insecureSSLSocketFactory = null;

    @Override
    public CloseableHttpClient createHttpClient(HttpHost httpHost,
            boolean acceptAnyCertificate, final int retries) {
        // TODO check if this is still necessary
        //disable logging by default to improve download performance
        //see issue 141 (https://github.com/michel-kraemer/gradle-download-task/issues/141)
        if (System.getProperty(LOG_PROPERTY) == null) {
            System.setProperty(LOG_PROPERTY, NoOpLog.class.getName());
        }

        HttpClientBuilder builder = HttpClientBuilder.create();

        //configure retries
        if (retries == 0) {
            builder.disableAutomaticRetries();
        } else {
            // TODO make interval configurable
            int maxRetries = retries;
            if (retries < 0) {
                maxRetries = Integer.MAX_VALUE;
            }
            builder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(
                    maxRetries, TimeValue.ofSeconds(0L)));
        }

        //configure proxy from system environment
        builder.setRoutePlanner(new SystemDefaultRoutePlanner(null));
        
        //accept any certificate if necessary
        if ("https".equals(httpHost.getSchemeName()) && acceptAnyCertificate) {
            SSLConnectionSocketFactory icsf = getInsecureSSLSocketFactory();
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

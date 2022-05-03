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
import org.gradle.api.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Default implementation of {@link HttpClientFactory}. Creates a new client
 * every time {@link #createHttpClient(HttpHost, boolean, int, Logger, boolean)}
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
            boolean acceptAnyCertificate, final int retries, Logger logger,
            boolean quiet) {
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
            builder.setRetryStrategy(new CustomHttpRequestRetryStrategy(
                    maxRetries, TimeValue.ofSeconds(0L), logger, quiet));
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

        if (logger.isDebugEnabled()) {
            DebugInterceptor di = new DebugInterceptor();
            // In contrast to a request interceptor, an exec interceptor can
            // be really added to the end of the exec chain just before the
            // main transport, so just before the request is made. This allows
            // it to record all headers. It can also intercept all requests
            // (including redirects, repeats, and authentication attempts)
            builder.addExecInterceptorLast("debug-interceptor", di);
            builder.addResponseInterceptorFirst(di);
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

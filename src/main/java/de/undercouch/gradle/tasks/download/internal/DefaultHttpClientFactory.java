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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

/**
 * Default implementation of {@link HttpClientFactory}. Creates a new client
 * every time {@link #createHttpClient(HttpHost, boolean, HttpRequestInterceptor, HttpResponseInterceptor)}
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
            boolean acceptAnyCertificate, HttpRequestInterceptor requestInterceptor,
            HttpResponseInterceptor responseInterceptor) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        
        //configure proxy from system environment
        builder.setRoutePlanner(new SystemDefaultRoutePlanner(null));
        
        //accept any certificate if necessary
        if ("https".equals(httpHost.getSchemeName()) && acceptAnyCertificate) {
            SSLConnectionSocketFactory icsf = getInsecureSSLSocketFactory();
            builder.setSSLSocketFactory(icsf);
            Registry<ConnectionSocketFactory> registry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", icsf)
                        .build();
            HttpClientConnectionManager cm =
                    new BasicHttpClientConnectionManager(registry);
            builder.setConnectionManager(cm);
        }
        
        //add an interceptor that replaces the invalid Content-Type
        //'none' by 'identity'
        builder.addInterceptorFirst(new ContentEncodingNoneInterceptor());
        
        //add interceptors
        if (requestInterceptor != null) {
            builder.addInterceptorLast(requestInterceptor);
        }
        if (responseInterceptor != null) {
            builder.addInterceptorLast(responseInterceptor);
        }

        CloseableHttpClient client = builder.build();
        return client;
    }
    
    private SSLConnectionSocketFactory getInsecureSSLSocketFactory() {
        if (insecureSSLSocketFactory == null) {
            SSLContext sc;
            try {
                sc = SSLContext.getInstance("SSL");
                sc.init(null, INSECURE_TRUST_MANAGERS, new SecureRandom());
                insecureSSLSocketFactory = new SSLConnectionSocketFactory(
                        sc, INSECURE_HOSTNAME_VERIFIER);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
        return insecureSSLSocketFactory;
    }
}

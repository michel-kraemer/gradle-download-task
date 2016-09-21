// Copyright 2013-2016 Michel Kraemer
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

package de.undercouch.gradle.tasks.download;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Tests if the download task plugin can download files through a proxy
 * @author Michel Kraemer
 */
public class ProxyTest extends TestBase {
    private static String PROXY_USERNAME = "testuser123";
    private static String PROXY_PASSWORD = "testpass456";
    
    private HttpProxyServer proxy;
    private int proxyPort;
    private int proxyCounter = 0;
    
    /**
     * Runs a proxy server counting requests
     * @param authenticating true if the proxy should require authentication
     * @throws Exception if an error occurred
     */
    private void startProxy(boolean authenticating) throws Exception {
        proxyPort = findPort();
        
        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    public HttpFilters filterRequest(HttpRequest originalRequest,
                            ChannelHandlerContext ctx) {
                       return new HttpFiltersAdapter(originalRequest) {
                          @Override
                          public void proxyToServerRequestSent() {
                              proxyCounter++;
                          }
                       };
                    }
                });
        
        if (authenticating) {
            bootstrap = bootstrap.withProxyAuthenticator(new ProxyAuthenticator() {
                @Override
                public boolean authenticate(String userName, String password) {
                    return PROXY_USERNAME.equals(userName) &&
                            PROXY_PASSWORD.equals(password);
                }

                @Override
                public String getRealm() {
                    return "gradle-download-task";
                }
            });
        }
        
        proxy = bootstrap.start();
    }
    
    /**
     * Stops the proxy server
     */
    private void stopProxy() {
        proxy.stop();
    }
    
    /**
     * <p>Find a setting for the "http.nonProxyHosts" system property that does
     * not bypass "localhost".</p>
     * <p>See http://bugs.java.com/view_bug.do?bug_id=6737819</p>
     * @return the new setting or <code>null</code> if no setting was found
     * on the current JVM
     * @throws Exception should never happen
     */
    private static String findNonProxyHosts() throws Exception {
        URI u = new URI("http://localhost");
        
        System.setProperty("http.nonProxyHosts", "");
        List<Proxy> l = ProxySelector.getDefault().select(u);
        assertFalse(l.isEmpty());
        if (l.get(0).type() != Proxy.Type.DIRECT) {
            return "";
        }
        
        System.setProperty("http.nonProxyHosts", "~localhost");
        l = ProxySelector.getDefault().select(u);
        assertFalse(l.isEmpty());
        if (l.get(0).type() != Proxy.Type.DIRECT) {
            return "~localhost";
        }
        
        return null;
    }
    
    /**
     * Tests if a single file can be downloaded through a proxy server
     * @param authenticating true if the proxy should require authentication
     * @param newNonProxyHosts new value of the "http.nonProxyHosts" system property
     * @param expectedProxyCounter number of times the request is expected to hit the proxy
     * @throws Exception if anything goes wrong
     */
    private void testProxy(boolean authenticating, String newNonProxyHosts, int expectedProxyCounter) throws Exception {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        String proxyUser = System.getProperty("http.proxyUser");
        String proxyPassword = System.getProperty("http.proxyPassword");
        
        startProxy(authenticating);
        try {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(this.proxyPort));
            
            if (newNonProxyHosts == null) {
                newNonProxyHosts = findNonProxyHosts();
            }
            if (newNonProxyHosts == null) {
                System.err.println("Could not configure nonProxyHosts that "
                        + "bypasses localhost. Please use a newer JDK version "
                        + "to run this test.");
                assumeTrue(false);
                return;
            }
            System.setProperty("http.nonProxyHosts", newNonProxyHosts);
            
            if (authenticating) {
                System.setProperty("http.proxyUser", PROXY_USERNAME);
                System.setProperty("http.proxyPassword", PROXY_PASSWORD);
            }
            
            Download t = makeProjectAndTask();
            t.src(makeSrc(TEST_FILE_NAME));
            File dst = folder.newFile();
            t.dest(dst);
            t.execute();
            
            byte[] dstContents = FileUtils.readFileToByteArray(dst);
            assertArrayEquals(contents, dstContents);
            assertEquals(expectedProxyCounter, proxyCounter);
        } finally {
            stopProxy();
            if (proxyHost == null) {
                System.getProperties().remove("http.proxyHost");
            } else {
                System.setProperty("http.proxyHost", proxyHost);
            }
            if (proxyPort == null) {
                System.getProperties().remove("http.proxyPort");
            } else {
                System.setProperty("http.proxyPort", proxyPort);
            }
            if (nonProxyHosts == null) {
                System.getProperties().remove("http.nonProxyHosts");
            } else {
                System.setProperty("http.nonProxyHosts", nonProxyHosts);
            }
            if (proxyUser == null) {
                System.getProperties().remove("http.proxyUser");
            } else {
                System.setProperty("http.proxyUser", proxyUser);
            }
            if (proxyPassword == null) {
                System.getProperties().remove("http.proxyPassword");
            } else {
                System.setProperty("http.proxyPassword", proxyPassword);
            }
        }
    }
    
    /**
     * Tests if a single file can be downloaded through a proxy server
     * @throws Exception if anything goes wrong
     */
    @Test
    public void normalProxy() throws Exception {
        testProxy(false, null, 1);
    }
    
    /**
     * Tests if a single file can be downloaded through a proxy server
     * @throws Exception if anything goes wrong
     */
    @Test
    public void authenticationProxy() throws Exception {
        testProxy(true, null, 1);
    }
    
    /**
     * Tests if the "http.nonProxyHosts" system property is respected 
     * @throws Exception if anything goes wrong
     */
    @Test
    public void nonProxyHosts() throws Exception {
        testProxy(false, "localhost", 0);
    }
}

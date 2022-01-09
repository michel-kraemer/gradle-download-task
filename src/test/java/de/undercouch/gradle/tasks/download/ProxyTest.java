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

package de.undercouch.gradle.tasks.download;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

/**
 * Tests if the download task plugin can download files through a proxy
 * @author Michel Kraemer
 */
public class ProxyTest extends TestBaseWithMockServer {
    private static String PROXY_USERNAME = "testuser123";
    private static String PROXY_PASSWORD = "testpass456";
    
    private HttpProxyServer proxy;
    private int proxyPort;
    private int proxyCounter = 0;

    /**
     * Host name of the local machine
     */
    private static String localHostName;

    /**
     * Find a free socket port
     * @return the number of the free port
     * @throws IOException if an IO error occurred
     */
    private static int findPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Gets the local host name to use for the tests
     * @throws UnknownHostException if the local host name could not be
     * resolved into an address
     * @throws SocketException if an I/O error occurs
     */
    @BeforeClass
    public static void setUpClass() throws UnknownHostException, SocketException {
        try {
            // noinspection ResultOfMethodCallIgnored
            InetAddress.getByName("localhost.localdomain");
            localHostName = "localhost.localdomain";
        } catch (UnknownHostException e) {
            localHostName = findSiteLocal();
            if (localHostName == null) {
                localHostName = InetAddress.getLocalHost().getCanonicalHostName();
            }
        }
    }

    /**
     * Get a site local IP4 address from the current node's interfaces
     * @return the IP address or {@code null} if the address could not
     * be determined
     * @throws SocketException if an I/O error occurs
     */
    private static String findSiteLocal() throws SocketException {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface n = interfaces.nextElement();
            Enumeration<InetAddress> addresses = n.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress i = addresses.nextElement();
                if (i.isSiteLocalAddress() && i instanceof Inet4Address) {
                    return i.getHostAddress();
                }
            }
        }
        return null;
    }
    
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
     * Tests if a single file can be downloaded through a proxy server
     * @param authenticating true if the proxy should require authentication
     * @throws Exception if anything goes wrong
     */
    private void testProxy(boolean authenticating) throws Exception {
        testProxy(authenticating, "", 1);
    }
    
    /**
     * Tests if a single file can be downloaded through a proxy server
     * @param authenticating true if the proxy should require authentication
     * @param newNonProxyHosts new value of the "http.nonProxyHosts" system property
     * @param expectedProxyCounter number of times the request is expected to hit the proxy
     * @throws Exception if anything goes wrong
     */
    private void testProxy(boolean authenticating, String newNonProxyHosts,
            int expectedProxyCounter) throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("content-length", String.valueOf(CONTENTS.length()))
                        .withBody(CONTENTS)));

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        String proxyUser = System.getProperty("http.proxyUser");
        String proxyPassword = System.getProperty("http.proxyPassword");
        
        startProxy(authenticating);
        try {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(this.proxyPort));
            System.setProperty("http.nonProxyHosts", newNonProxyHosts);
            
            if (authenticating) {
                System.setProperty("http.proxyUser", PROXY_USERNAME);
                System.setProperty("http.proxyPassword", PROXY_PASSWORD);
            }
            
            Download t = makeProjectAndTask();
            t.src("http://" + localHostName + ":" + wireMockRule.port() + "/" + TEST_FILE_NAME);
            File dst = folder.newFile();
            t.dest(dst);
            execute(t);
            
            String dstContents = FileUtils.readFileToString(dst, StandardCharsets.UTF_8);
            assertEquals(CONTENTS, dstContents);
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
        testProxy(false);
    }
    
    /**
     * Tests if a single file can be downloaded through a proxy server
     * @throws Exception if anything goes wrong
     */
    @Test
    public void authenticationProxy() throws Exception {
        testProxy(true);
    }
    
    /**
     * Tests if the "http.nonProxyHosts" system property is respected 
     * @throws Exception if anything goes wrong
     */
    @Test
    public void nonProxyHosts() throws Exception {
        testProxy(false, localHostName, 0);
        testProxy(false, "example.com", 1);
    }
}

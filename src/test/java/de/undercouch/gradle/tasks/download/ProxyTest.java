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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Tests if the download task plugin can download files through a proxy
 * @author Michel Kraemer
 */
public class ProxyTest extends TestBase {
    private static HttpProxyServer proxy;
    private static int proxyPort;
    private static int proxyCounter = 0;
    
    /**
     * Runs a proxy server counting requests
     * @throws Exception if the proxy server could not be started
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        proxyPort = findPort();
        proxy = DefaultHttpProxyServer.bootstrap()
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
                })
                .start();
    }
    
    /**
     * Stops the proxy server
     */
    @AfterClass
    public static void tearDownClass() {
        proxy.stop();
    }
    
    /**
     * Set up unit tests
     * @throws Exception if an error occurred
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // reset proxy request counter
        proxyCounter = 0;
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
     * @throws Exception if anything goes wrong
     */
    @Test
    public void proxy() throws Exception {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        
        try {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(ProxyTest.proxyPort));
            String newNonProxyHosts = findNonProxyHosts();
            if (newNonProxyHosts == null) {
                System.err.println("Could not configure nonProxyHosts that "
                        + "bypasses localhost. Please use a newer JDK version "
                        + "to run this test.");
                assumeTrue(false);
                return;
            }
            System.setProperty("http.nonProxyHosts", newNonProxyHosts);
            
            Download t = makeProjectAndTask();
            t.src(makeSrc(TEST_FILE_NAME));
            File dst = folder.newFile();
            t.dest(dst);
            t.execute();
            
            byte[] dstContents = FileUtils.readFileToByteArray(dst);
            assertArrayEquals(contents, dstContents);
            assertEquals(1, proxyCounter);
        } finally {
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
        }
    }
}

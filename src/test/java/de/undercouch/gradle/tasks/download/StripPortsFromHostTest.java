package de.undercouch.gradle.tasks.download;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Test if default HTTP ports are stripped from the {@code Host} header by
 * {@link de.undercouch.gradle.tasks.download.internal.StripPortsFromHostInterceptor}.</p>
 *
 * <p>Since we cannot set up a mock HTTP server on port 80 or 443, our approach
 * here is to configure a proxy server that forwards requests to WireMock
 * running on another port.</p>
 */
public class StripPortsFromHostTest extends TestBase {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .keystorePath(SslTest.class.getResource("/keystore").toString())
                    .keystorePassword("gradle")
                    .keyManagerPassword("gradle")
                    .jettyStopTimeout(10000L))
            .configureStaticDsl(true)
            .build();

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

    private void runWithProxy(String scheme, Runnable r) throws IOException {
        int port = findPort();
        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
                .withPort(port)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    public HttpFilters filterRequest(HttpRequest originalRequest,
                            ChannelHandlerContext ctx) {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public InetSocketAddress proxyToServerResolutionStarted(
                                    String resolvingServerHostAndPort) {
                                // always resolve to WireMock
                                if (scheme.equals("https")) {
                                    return new InetSocketAddress("localhost", wireMock.getHttpsPort());
                                } else {
                                    return new InetSocketAddress("localhost", wireMock.getPort());
                                }
                            }
                        };
                    }
                });

        HttpProxyServer proxy = bootstrap.start();
        try {
            String proxyHost = System.getProperty(scheme + ".proxyHost");
            String proxyPort = System.getProperty(scheme + ".proxyPort");
            String nonProxyHosts = System.getProperty(scheme + ".nonProxyHosts");
            String proxyUser = System.getProperty(scheme + ".proxyUser");
            String proxyPassword = System.getProperty(scheme + ".proxyPassword");
            try {
                System.setProperty(scheme + ".proxyHost", "127.0.0.1");
                System.setProperty(scheme + ".proxyPort", String.valueOf(port));
                System.getProperties().remove(scheme + ".nonProxyHosts");
                System.getProperties().remove(scheme + ".proxyUser");
                System.getProperties().remove(scheme + ".proxyPassword");

                r.run();
            } finally {
                if (proxyHost == null) {
                    System.getProperties().remove(scheme + ".proxyHost");
                } else {
                    System.setProperty(scheme + ".proxyHost", proxyHost);
                }
                if (proxyPort == null) {
                    System.getProperties().remove(scheme + ".proxyPort");
                } else {
                    System.setProperty(scheme + ".proxyPort", proxyPort);
                }
                if (nonProxyHosts == null) {
                    System.getProperties().remove(scheme + ".nonProxyHosts");
                } else {
                    System.setProperty(scheme + ".nonProxyHosts", nonProxyHosts);
                }
                if (proxyUser == null) {
                    System.getProperties().remove(scheme + ".proxyUser");
                } else {
                    System.setProperty(scheme + ".proxyUser", proxyUser);
                }
                if (proxyPassword == null) {
                    System.getProperties().remove(scheme + ".proxyPassword");
                } else {
                    System.setProperty(scheme + ".proxyPassword", proxyPassword);
                }
            }
        } finally {
            proxy.stop();
        }
    }

    private void simpleTest(String scheme, String host) throws IOException {
        simpleTest(scheme, host, null);
    }

    private void simpleTest(String scheme, String host, String explicitHostHeader) throws IOException {
        File dst = newTempFile();

        runWithProxy(scheme, () -> {
            String expected = "Hello world";

            String expectedHostHeader = explicitHostHeader != null ? explicitHostHeader : host;
            stubFor(get(urlEqualTo("/"))
                    .withHeader("Host", equalTo(expectedHostHeader))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(expected)));

            Download t = makeProjectAndTask();
            t.src(scheme + "://" + host);
            t.dest(dst);
            if (scheme.equals("https")) {
                t.acceptAnyCertificate(true);
            }
            if (explicitHostHeader != null) {
                t.header("Host", explicitHostHeader);
            }

            execute(t);

            verify(1, getRequestedFor(urlEqualTo("/"))
                    .withHeader("Host", equalTo(expectedHostHeader)));
            assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(expected);
        });
    }

    @Test
    public void stripDefaultHttpPort() throws IOException {
        simpleTest("http", "example.invalidtld");
    }

    @Test
    public void stripDefaultHttpsPort() throws IOException {
        simpleTest("https", "example.invalidtld");
    }

    @Test
    public void doNotStripNonDefaultPort() throws IOException {
        simpleTest("http", "example.invalidtld:8080");
    }

    @Test
    public void doNotStripDefaultPortIfExplicitHeader() throws IOException {
        simpleTest("http", "example.invalidtld", "example.invalidtld:80");
    }
}

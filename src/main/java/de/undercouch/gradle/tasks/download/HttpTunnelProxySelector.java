package de.undercouch.gradle.tasks.download;

import java.io.IOException;
import java.net.*;
import java.util.List;

import static java.net.Proxy.NO_PROXY;

public class HttpTunnelProxySelector extends ProxySelector {

    private static final
    ProxySelector defaultProxySelector = getDefault();

    @Override
    public List<Proxy> select(URI uri) {
        try {
            URI httpUri = new URI("http", uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment());
            List<Proxy> proxies = defaultProxySelector.select(httpUri);
            if(proxies.get(0).equals(NO_PROXY)) {
                return defaultProxySelector.select(uri);
            }
            return proxies;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        defaultProxySelector.connectFailed(uri, sa, ioe);
    }
}


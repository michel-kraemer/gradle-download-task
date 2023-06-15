package de.undercouch.gradle.tasks.download.internal;


import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.ProtocolException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

public class SanitizingLocationUriRedirectStrategy extends DefaultRedirectStrategy {
    @Override
    protected URI createLocationURI(String location) throws ProtocolException {
        return super.createLocationURI(sanitizeUrl(location));
    }

    private String sanitizeUrl(String sanitizeURL) throws ProtocolException {
        URI uri;
        try {
            URL url = new URL(URLDecoder.decode(sanitizeURL, "UTF-8"));
            // https://stackoverflow.com/a/8962879/956415
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (URISyntaxException | MalformedURLException | UnsupportedEncodingException e) {
            throw new ProtocolException(e.getMessage(), e);
        }

        return uri.toString();
    }
}



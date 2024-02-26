package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * <p>A HTTP request interceptor that removes sensitive headers from the request
 * after the client has been redirected to another host.</p>
 *
 * <p>This interceptor should be put at the end of the request interceptor list
 * so it has access to all headers that other interceptors have set.</p>
 *
 * <p>See <a href="https://github.com/michel-kraemer/gradle-download-task/issues/382">https://github.com/michel-kraemer/gradle-download-task/issues/382</a>
 * for more information. Also see <a href="https://github.com/ducaale/xh/blob/df2040e34dccaf93b7978d6e8b3404a77e984432/src/redirect.rs#L95-L101">
 * https://github.com/ducaale/xh/blob/df2040e34dccaf93b7978d6e8b3404a77e984432/src/redirect.rs#L95-L101</a>
 * for a reference how other HTTP client do this.</p>
 *
 * @author Michel Kraemer
 */
public class RemoveSensitiveHeadersInterceptor implements HttpRequestInterceptor {
    private final HttpHost originalHost;

    /**
     * Constructs a new interceptor
     * @param originalHost the original host against which the first request
     * will be made
     */
    public RemoveSensitiveHeadersInterceptor(HttpHost originalHost) {
        this.originalHost = originalHost;
    }

    private int portOrDefault(int port, String scheme) {
        if (port != -1) {
            return port;
        }
        if ("https".equals(scheme)) {
            return 443;
        } else if ("http".equals(scheme)) {
            return 80;
        }
        return -1;
    }

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) {
        int originalPort = portOrDefault(originalHost.getPort(), originalHost.getSchemeName());
        int requestPort = portOrDefault(request.getAuthority().getPort(), request.getScheme());
        if (request.getAuthority() == null ||
                !request.getAuthority().getHostName().equals(originalHost.getHostName()) ||
                requestPort != originalPort) {
            request.removeHeaders(HttpHeaders.AUTHORIZATION);
            request.removeHeaders(HttpHeaders.COOKIE);
            request.removeHeaders("Cookie2");
            request.removeHeaders(HttpHeaders.PROXY_AUTHORIZATION);
            request.removeHeaders(HttpHeaders.WWW_AUTHENTICATE);
        }
    }
}

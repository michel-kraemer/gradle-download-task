package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.RequestTargetHost;

import java.util.Map;

/**
 * <p>A HTTP request interceptor that strips the default ports :80 and :443 from
 * the request's {@code Host} header unless the host has been explicitly
 * specified by the user.</p>
 *
 * <p>This interceptor should be put at the end of the request interceptor list.
 * It modifies the {@code Host} header set by the {@link RequestTargetHost}
 * interceptor, which always adds a port to the host even if it is a default
 * one. According to the HTTP specification, default ports can be omitted.
 * Other HTTP clients such as curl, wget, or web browsers do not include the
 * port, so we should not do that either. For more information, see
 * <a href="https://github.com/michel-kraemer/gradle-download-task/issues/241">https://github.com/michel-kraemer/gradle-download-task/issues/241</a>.</p>
 *
 * @author Michel Kraemer
 */
public class StripPortsFromHostInterceptor implements HttpRequestInterceptor {
    private final Map<String, String> headers;

    /**
     * Create a new interceptor
     * @param headers the headers specified by the user
     */
    public StripPortsFromHostInterceptor(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) {
        if (headers != null && headers.containsKey(HttpHeaders.HOST)) {
            // The host header has been specified by the user. Do not change it.
            return;
        }

        Header host = request.getFirstHeader(HttpHeaders.HOST);
        String strHost = host.getValue();
        String scheme = request.getScheme();
        if (scheme.equals("http") && strHost.endsWith(":80")) {
            request.setHeader(HttpHeaders.HOST,
                    strHost.substring(0, strHost.length() - 3));
        } else if (scheme.equals("https") && strHost.endsWith(":443")) {
            request.setHeader(HttpHeaders.HOST,
                    strHost.substring(0, strHost.length() - 4));
        }
    }
}

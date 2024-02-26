package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Interceptor that logs request and response headers if debug logging is enabled
 * @author Michel Kraemer
 */
public class DebugInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
    private final Logger logger = Logging.getLogger(DebugInterceptor.class);

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) {
        StringBuilder sb = new StringBuilder("Sending request:\n");
        sb.append("> ")
                .append(request.getMethod()).append(" ")
                .append(request.getRequestUri()).append("\n");
        for (Header h : request.getHeaders()) {
            sb.append("> ").append(h.toString()).append("\n");
        }
        logger.debug(sb.toString());
    }

    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) {
        StringBuilder sb = new StringBuilder("Received response:\n");
        sb.append("< ")
                .append(response.getVersion()).append(" ")
                .append(response.getCode()).append(" ")
                .append(response.getReasonPhrase()).append("\n");
        for (Header h : response.getHeaders()) {
            sb.append("< ").append(h.toString()).append("\n");
        }
        logger.debug(sb.toString());
    }
}

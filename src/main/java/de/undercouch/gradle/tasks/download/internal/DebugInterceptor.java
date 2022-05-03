package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;

/**
 * Interceptor that logs request and response headers if debug logging is enabled
 * @author Michel Kraemer
 */
public class DebugInterceptor implements ExecChainHandler, HttpResponseInterceptor {
    private final Logger logger = Logging.getLogger(DebugInterceptor.class);

    @Override
    public ClassicHttpResponse execute(ClassicHttpRequest request,
            ExecChain.Scope scope, ExecChain chain) throws IOException, HttpException {
        StringBuilder sb = new StringBuilder("Sending request:\n");
        sb.append("> ")
                .append(request.getMethod()).append(" ")
                .append(request.getRequestUri()).append("\n");
        for (Header h : request.getHeaders()) {
            sb.append("> ").append(h.toString()).append("\n");
        }
        logger.debug(sb.toString());
        return chain.proceed(request, scope);
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

package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;

/**
 * A custom strategy that logs every retry attempt and retries requests on any
 * exception but {@link UnknownHostException}
 * @author Michel Kraemer
 */
public class CustomHttpRequestRetryStrategy extends DefaultHttpRequestRetryStrategy {
    private final Logger logger;
    private final boolean quiet;
    private final int maxRetries;

    public CustomHttpRequestRetryStrategy(final int maxRetries,
            final TimeValue defaultRetryInterval, Logger logger, boolean quiet) {
        super(maxRetries, defaultRetryInterval,
                Collections.singletonList(UnknownHostException.class),
                Arrays.asList(HttpStatus.SC_TOO_MANY_REQUESTS,
                        HttpStatus.SC_SERVICE_UNAVAILABLE));
        this.logger = logger;
        this.quiet = quiet;
        this.maxRetries = maxRetries;
    }

    private void logRetry(int execCount) {
        if (!quiet) {
            logger.warn("Request attempt " + execCount + "/" + maxRetries +
                    " failed. Retrying ...");
        }
    }

    @Override
    public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
        boolean res = super.retryRequest(response, execCount, context);
        if (res) {
            logRetry(execCount);
            logger.debug("Status code: " + response.getCode());
            logger.debug("Status message: " + response.getReasonPhrase());
        }
        return res;
    }

    @Override
    public boolean retryRequest(HttpRequest request, IOException exception,
            int execCount, HttpContext context) {
        boolean res = super.retryRequest(request, exception, execCount, context);
        if (res) {
            logRetry(execCount);
            logger.debug("Request attempt failed", exception);
        }
        return res;
    }
}

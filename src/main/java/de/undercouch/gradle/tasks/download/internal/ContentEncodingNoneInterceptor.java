package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;

import java.io.IOException;

/**
 * <p>Intercepts HTTP responses and replaces the invalid Content-Encoding 'none'
 * with 'identity'.</p>
 * <p>See <a href="https://github.com/michel-kraemer/gradle-download-task/issues/55">issue #55</a></p>
 */
public class ContentEncodingNoneInterceptor implements ExecChainHandler {
    private static final String IDENTITY = "identity";
    private static final Header IDENTITY_HEADER =
            new BasicHeader(HttpHeaders.CONTENT_ENCODING, IDENTITY);

    /**
     * Check if the value of the given header equals 'none'
     * @param h the header
     * @return true if the value equals 'none', false otherwise
     */
    private static boolean isNone(Header h) {
        return h != null && isNone(h.getValue());
    }

    /**
     * Check if the value of the given header equals 'none'
     * @param h the header
     * @return true if the value equals 'none', false otherwise
     */
    private static boolean isNone(String h) {
        return "none".equalsIgnoreCase(h);
    }

    @Override
    public ClassicHttpResponse execute(ClassicHttpRequest request,
            ExecChain.Scope scope, ExecChain chain) throws IOException,
            HttpException {
        ClassicHttpResponse response = chain.proceed(request, scope);

        // replace invalid 'Content-Encoding' header
        Header[] hs = response.getHeaders(HttpHeaders.CONTENT_ENCODING);
        if (hs != null) {
            for (Header h : hs) {
                if (isNone(h)) {
                    response.removeHeaders(HttpHeaders.CONTENT_ENCODING);
                    response.addHeader(IDENTITY_HEADER);
                    break;
                }
            }
        }

        // replace (cached) Content-Encoding in HttpEntity
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String ce = entity.getContentEncoding();
            if (isNone(ce)) {
                response.setEntity(new CustomContentEncodingEntity(entity,
                        IDENTITY));
            }
        }

        return response;
    }
}

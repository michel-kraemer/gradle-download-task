package de.undercouch.gradle.tasks.download.internal;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;

/**
 * Wraps around {@link HttpEntity} and replaces its content type
 */
public class CustomContentEncodingEntity extends HttpEntityWrapper {
    private final String contentEncoding;

    /**
     * Create the HTTP entity
     * @param wrappedEntity the entity to wrap around
     * @param contentEncoding the custom content encoding
     */
    public CustomContentEncodingEntity(HttpEntity wrappedEntity,
            String contentEncoding) {
        super(wrappedEntity);
        this.contentEncoding = contentEncoding;
    }

    @Override
    public String getContentEncoding() {
        return contentEncoding;
    }
}

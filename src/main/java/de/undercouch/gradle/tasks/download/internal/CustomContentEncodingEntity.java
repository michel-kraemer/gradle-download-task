// Copyright 2016 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.gradle.tasks.download.internal;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * Wraps around {@link HttpEntity} and replaces its content type
 * @author Michel Kraemer
 */
public class CustomContentEncodingEntity extends HttpEntityWrapper {
    private final Header contentEncoding;

    /**
     * Create the HTTP entity
     * @param wrappedEntity the entity to wrap around
     * @param contentEncoding the custom content encoding
     */
    public CustomContentEncodingEntity(HttpEntity wrappedEntity,
            Header contentEncoding) {
        super(wrappedEntity);
        this.contentEncoding = contentEncoding;
    }

    @Override
    public Header getContentEncoding() {
        return contentEncoding;
    }
}

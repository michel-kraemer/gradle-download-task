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

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * Intercepts HTTP responses and replaces the invalid Content-Encoding
 * 'none' by 'identity'.
 * @author Michel Kraemer
 */
public class ContentEncodingNoneInterceptor implements HttpResponseInterceptor {
    private static final Header IDENTITY =
            new BasicHeader(HTTP.CONTENT_ENCODING, "identity");

    /**
     * Check if the value of the given header equals 'none'
     * @param h the header
     * @return true if the value equals 'none', false otherwise
     */
    private static boolean isNone(Header h) {
        return h != null && h.getValue().equalsIgnoreCase("none");
    }

    @Override
    public void process(HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        //replace invalid 'Content-Encoding' header
        Header[] hs = response.getHeaders(HTTP.CONTENT_ENCODING);
        if (hs != null) {
            for (Header h : hs) {
                if (isNone(h)) {
                    response.removeHeaders(HTTP.CONTENT_ENCODING);
                    response.addHeader(IDENTITY);
                    break;
                }
            }
        }

        //replace (cached) Content-Encoding in HttpEntity
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            Header ce = entity.getContentEncoding();
            if (isNone(ce)) {
                response.setEntity(new CustomContentEncodingEntity(
                        entity, IDENTITY));
            }
        }
    }
}

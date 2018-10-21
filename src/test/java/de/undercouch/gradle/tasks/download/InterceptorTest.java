// Copyright 2013-2017 Michel Kraemer
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

package de.undercouch.gradle.tasks.download;

import de.undercouch.gradle.tasks.download.org.apache.http.HttpException;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpRequest;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpRequestInterceptor;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpResponse;
import de.undercouch.gradle.tasks.download.org.apache.http.HttpResponseInterceptor;
import de.undercouch.gradle.tasks.download.org.apache.http.entity.StringEntity;
import de.undercouch.gradle.tasks.download.org.apache.http.protocol.HttpContext;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test if interceptors are called correctly
 * @author Michel Kraemer
 */
public class InterceptorTest extends TestBase {
    private static final String INTERCEPTOR = "interceptor";
    private static final String UNINTERCEPTED = "UNINTERCEPTED";
    private static final String INTERCEPTED = "INTERCEPTED";
    private static final String ADDITIONAL_REQUEST_HEADER_KEY = "X-My-Header";
    private static final String ADDITIONAL_REQUEST_HEADER_VALUE = "1234";

    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler contentLengthHandler = new ContextHandler("/" + INTERCEPTOR) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                response.setStatus(200);
                
                String message = UNINTERCEPTED;
                String addValue = request.getHeader(ADDITIONAL_REQUEST_HEADER_KEY);
                if (addValue != null) {
                    message += ":" + addValue;
                }
                
                PrintWriter rw = response.getWriter();
                rw.write(message);
                rw.close();
            }
        };
        return new Handler[] { contentLengthHandler };
    }

    /**
     * Tests if an interceptor can be used to manipulate a request before
     * it is sent
     * @throws Exception if anything goes wrong
     */
    @Test
    public void interceptRequest() throws Exception {
        final AtomicBoolean interceptorCalled = new AtomicBoolean(false);

        Download t = makeProjectAndTask();
        t.src(makeSrc(INTERCEPTOR));
        File dst = folder.newFile();
        t.dest(dst);
        t.requestInterceptor(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context)
                    throws HttpException, IOException {
                assertFalse(interceptorCalled.get());
                interceptorCalled.set(true);
                request.addHeader(ADDITIONAL_REQUEST_HEADER_KEY,
                        ADDITIONAL_REQUEST_HEADER_VALUE);
            }
        });
        t.execute();
        
        assertTrue(interceptorCalled.get());
        
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(UNINTERCEPTED + ":" + ADDITIONAL_REQUEST_HEADER_VALUE,
                dstContents);
    }
    
    /**
     * Tests if we can manipulate a response
     * @throws Exception if anything goes wrong
     */
    @Test
    public void interceptResponse() throws Exception {
        final AtomicBoolean interceptorCalled = new AtomicBoolean(false);

        Download t = makeProjectAndTask();
        t.src(makeSrc(INTERCEPTOR));
        File dst = folder.newFile();
        t.dest(dst);
        t.responseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                assertFalse(interceptorCalled.get());
                interceptorCalled.set(true);
                response.setEntity(new StringEntity(INTERCEPTED));
            }
        });
        t.execute();
        
        assertTrue(interceptorCalled.get());
        
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(INTERCEPTED, dstContents);
    }
}

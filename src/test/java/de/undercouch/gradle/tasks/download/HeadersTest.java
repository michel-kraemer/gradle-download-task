// Copyright 2013-2016 Michel Kraemer
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if HTTP headers can be sent
 * @author Michel Kraemer
 */
public class HeadersTest extends TestBase {
    private static final String HEADERS = "headers:";
    private static final String ECHO_HEADERS = "echo-headers";
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        //echo X-* headers back in response body
        ContextHandler echoHeadersHandler = new ContextHandler("/" + ECHO_HEADERS) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                response.setStatus(200);
                PrintWriter rw = response.getWriter();
                rw.write(HEADERS + "\n");
                @SuppressWarnings("unchecked")
                Enumeration<String> headerNames = (Enumeration<String>)request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    if (name.startsWith("X-")) {
                        rw.write(String.format("  %s: %s\n", name, request.getHeader(name)));
                    }
                }
                rw.close();
            }
        };
        
        Handler[] superHandlers = super.makeHandlers();
        Handler[] handlers = new Handler[superHandlers.length + 1];
        handlers[0] = echoHeadersHandler;
        System.arraycopy(superHandlers, 0, handlers, 1, superHandlers.length);
        return handlers;
    }
    
    /**
     * Tests that no headers request headers are set when not specified
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithNoHeaders() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(ECHO_HEADERS));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(HEADERS + "\n", dstContents);
    }

    /**
     * Tests that specified request headers are included
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithHeaders() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(ECHO_HEADERS));
        File dst = folder.newFile();
        t.dest(dst);
        t.header("X-Header-Test-A", "value A");
        t.header("X-Header-Test-B", "value B");
        t.execute();
        
        assertEquals(2, t.getHeaders().size());
        assertEquals("value A", t.getHeader("X-Header-Test-A"));
        assertEquals("value B", t.getHeader("X-Header-Test-B"));

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(HEADERS + "\n  X-Header-Test-A: value A\n  "
                + "X-Header-Test-B: value B\n", dstContents);
    }
    
    /**
     * Tests that request headers can be set
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithHeadersMap() throws Exception {
        Download t = makeProjectAndTask();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Header-Test-A", "value A");
        headers.put("X-Header-Test-B", "value B");
        t.headers(headers);
        
        assertEquals(2, t.getHeaders().size());
        assertEquals("value A", t.getHeader("X-Header-Test-A"));
        assertEquals("value B", t.getHeader("X-Header-Test-B"));
        
        t.headers(null);
        assertEquals(0, t.getHeaders().size());
    }
}

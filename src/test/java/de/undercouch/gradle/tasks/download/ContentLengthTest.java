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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if the plugin can handle invalid or missing Content-Length header
 * @author Michel Kraemer
 */
public class ContentLengthTest extends TestBase {
    private static final String CONTENT_LENGTH = "content-length";
    private String contentLength;
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler contentLengthHandler = new ContextHandler("/" + CONTENT_LENGTH) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                response.setStatus(200);
                if (contentLength != null) {
                    response.setHeader("Content-Length", contentLength);
                }
                PrintWriter rw = response.getWriter();
                rw.write("cl: " + String.valueOf(contentLength));
                rw.close();
            }
        };
        return new Handler[] { contentLengthHandler };
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        contentLength = null;
    }
    
    /**
     * Tests if the plugin can handle a missing Content-Length header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingContentLength() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(CONTENT_LENGTH));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("cl: null", dstContents);
    }
    
    /**
     * Tests if the plugin can handle an incorrect Content-Length header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectContentLength() throws Exception {
        contentLength = "10000";
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(CONTENT_LENGTH));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("cl: 10000", dstContents);
    }
}

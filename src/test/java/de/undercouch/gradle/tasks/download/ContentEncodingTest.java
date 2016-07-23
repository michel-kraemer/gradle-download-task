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
 * Tests if the plugin can handle invalid or missing Content-Encoding header
 * @author Michel Kraemer
 */
public class ContentEncodingTest extends TestBase {
    private static final String CONTENT_ENCODING = "content-encoding";
    private String contentEncoding;
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler contentEncodingHandler = new ContextHandler("/" + CONTENT_ENCODING) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                response.setStatus(200);
                if (contentEncoding != null) {
                    response.setHeader("Content-Encoding", contentEncoding);
                }
                PrintWriter rw = response.getWriter();
                rw.write("ce: " + String.valueOf(contentEncoding));
                rw.close();
            }
        };
        return new Handler[] { contentEncodingHandler };
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        contentEncoding = null;
    }
    
    /**
     * Tests if the plugin can handle the invalid value 'none'
     * @throws Exception if anything goes wrong
     */
    @Test
    public void contentEncodingNone() throws Exception {
        contentEncoding = "None";
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(CONTENT_ENCODING));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("ce: None", dstContents);
    }
}

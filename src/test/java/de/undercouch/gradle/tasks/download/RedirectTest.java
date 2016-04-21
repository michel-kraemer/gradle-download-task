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
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if the plugin can handle redirects
 * @author Michel Kraemer
 */
public class RedirectTest extends TestBase {
    private static final String REDIRECT = "redirect";
    private int redirects = 0;
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler redirectHandler = new ContextHandler("/" + REDIRECT) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                if (redirects > 0) {
                    redirects--;
                    response.sendRedirect("/" + REDIRECT);
                } else {
                    response.setStatus(200);
                    PrintWriter rw = response.getWriter();
                    rw.write("r: " + redirects);
                    rw.close();
                }
            }
        };
        return new Handler[] { redirectHandler };
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        redirects = 0;
    }
    
    /**
     * Tests if the plugin can handle one redirect
     * @throws Exception if anything goes wrong
     */
    @Test
    public void oneRedirect() throws Exception {
        redirects = 1;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(REDIRECT));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("r: 0", dstContents);
    }
    
    /**
     * Tests if the plugin can handle ten redirects
     * @throws Exception if anything goes wrong
     */
    @Test
    public void tenRedirect() throws Exception {
        redirects = 10;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(REDIRECT));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("r: 0", dstContents);
    }
    
    /**
     * Make sure the plugin fails with too many redirects
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void tooManyRedirects() throws Exception {
        redirects = 31;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(REDIRECT));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
    }
}

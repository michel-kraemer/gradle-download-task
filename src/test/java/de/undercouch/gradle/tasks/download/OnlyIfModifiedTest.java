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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if the plugin handles the last-modified header correctly
 * @author Michel Kraemer
 */
public class OnlyIfModifiedTest extends TestBase {
    private static final String LAST_MODIFIED = "last-modified";
    private String lastModified;
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler lastModifiedHandler = new ContextHandler("/" + LAST_MODIFIED) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                response.setStatus(200);
                if (lastModified != null) {
                    response.setHeader("Last-Modified", lastModified);
                }
                PrintWriter rw = response.getWriter();
                rw.write("lm: " + String.valueOf(lastModified));
                rw.close();
            }
        };
        return new Handler[] { lastModifiedHandler };
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        lastModified = null;
    }
    
    /**
     * Tests if the plugin can handle a missing Last-Modified header and still
     * downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingLastModified() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(LAST_MODIFIED));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("lm: null", dstContents);
    }
    
    /**
     * Tests if the plugin can handle an incorrect Last-Modified header and
     * still downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectContentLength() throws Exception {
        lastModified = "abcd";
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(LAST_MODIFIED));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("lm: abcd", dstContents);
    }
    
    /**
     * Tests if the plugin downloads a file and sets the timestamp correctly
     * @throws Exception if anything goes wrong
     */
    @Test
    public void setFileLastModified() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();
        lastModified = lm;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(LAST_MODIFIED));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.execute();

        assertTrue(dst.exists());
        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong, lmlong);
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("lm: " + lm, dstContents);
    }
    
    /**
     * Tests if the plugin doesn't download a file if the timestamp equals
     * the last-modified header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void dontDownloadIfEqual() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();
        lastModified = lm;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(LAST_MODIFIED));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");
        dst.setLastModified(expectedlmlong);
        t.dest(dst);
        t.onlyIfModified(true);
        t.execute();

        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong, lmlong);
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Hello", dstContents);
    }
    
    /**
     * Tests if the plugin doesn't download a file if the timestamp is newer
     * than the last-modified header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void dontDownloadIfOlder() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();
        lastModified = lm;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(LAST_MODIFIED));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");
        dst.setLastModified(expectedlmlong + 1000);
        t.dest(dst);
        t.onlyIfModified(true);
        t.execute();

        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong + 1000, lmlong);
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Hello", dstContents);
    }
    
    /**
     * Tests if the plugin downloads a file if the timestamp is older than
     * the last-modified header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void newerDownload() throws Exception {
        String lm = "Tue, 15 Nov 1994 12:45:26 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();
        lastModified = lm;
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(LAST_MODIFIED));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");
        dst.setLastModified(expectedlmlong - 1000);
        t.dest(dst);
        t.onlyIfModified(true);
        t.execute();

        long lmlong = dst.lastModified();
        assertEquals(expectedlmlong, lmlong);
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("lm: " + lm, dstContents);
    }
}

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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if the plugin can handle compressed content
 * @author Michel Kraemer
 */
public class CompressionTest extends TestBase {
    private static final String COMPRESSED = "compressed";
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler compressionHandler = new ContextHandler("/" + COMPRESSED) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                String acceptEncoding = request.getHeader("Accept-Encoding");
                boolean acceptGzip = "gzip".equals(acceptEncoding);
                
                response.setStatus(200);
                OutputStream os = response.getOutputStream();
                if (acceptGzip) {
                    response.setHeader("Content-Encoding", "gzip");
                    GZIPOutputStream gos = new GZIPOutputStream(os);
                    OutputStreamWriter osw = new OutputStreamWriter(gos);
                    osw.write("Compressed");
                    osw.close();
                    gos.flush();
                    gos.close();
                } else {
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    osw.write("Uncompressed");
                    osw.close();
                }
                os.close();
            }
        };
        return new Handler[] { compressionHandler };
    }
    
    /**
     * Tests if the plugin can handle compressed content
     * @throws Exception if anything goes wrong
     */
    @Test
    public void compressed() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(COMPRESSED));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Compressed", dstContents);
    }
    
    /**
     * Tests if the plugin can request uncompressed content
     * @throws Exception if anything goes wrong
     */
    @Test
    public void uncompressed() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(COMPRESSED));
        File dst = folder.newFile();
        t.dest(dst);
        t.compress(false);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Uncompressed", dstContents);
    }
}

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Test;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.security.SslSocketConnector;

/**
 * Tests if the plugin can handle HTTPS
 * @author Michel Kraemer
 */
public class SslTest extends TestBase {
    private static final String SSL = "ssl";
    
    @Override
    protected Server createServer() {
        Server server = new Server();
        
        SslSocketConnector connector = new SslSocketConnector();
        connector.setKeystore(this.getClass().getResource("/keystore").toString());
        connector.setKeyPassword("gradle");
        
        //run server on any free port
        connector.setPort(0);
        
        server.setConnectors(new Connector[] { connector });
        
        return server;
    }
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler sslHandler = new ContextHandler("/" + SSL) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                response.setStatus(200);
                PrintWriter rw = response.getWriter();
                rw.write("Hello");
                rw.close();
            }
        };
        return new Handler[] { sslHandler };
    }
    
    /**
     * Tests if the plugin can fetch a resource from a HTTPS URL accepting
     * any certificate
     * @throws Exception if anything goes wrong
     */
    @Test
    public void acceptAnyCertificate() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(SSL).replace("http", "https"));
        File dst = folder.newFile();
        t.dest(dst);
        t.acceptAnyCertificate(true);
        assertTrue(t.isAcceptAnyCertificate());
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Hello", dstContents);
    }
    
    /**
     * Tests if connecting to a HTTPS URL fails if the certificate is unknown
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void unknownCertificate() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(SSL).replace("http", "https"));
        File dst = folder.newFile();
        t.dest(dst);
        assertFalse(t.isAcceptAnyCertificate());
        t.execute();
    }
}

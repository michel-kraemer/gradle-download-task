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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if the plugin can access a resource that requires authentication
 * @author Michel Kraemer
 */
public class AuthenticationTest extends TestBase {
    private static final String PASSWORD = "testpass456";
    private static final String USERNAME = "testuser123";
    private static final String AUTHENTICATE = "authenticate";
    public static final String INVALID_AUTHENTICATION_TYPE = "Invalid Authentication";

    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler authenticationHandler = new ContextHandler("/" + AUTHENTICATE) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                String ahdr = request.getHeader("Authorization");
                if (ahdr == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "No authorization header given");
                    return;
                }
                if (!ahdr.startsWith("Basic ")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Authorization header does not start with 'Basic '" + ahdr);
                    return;
                }
                
                ahdr = ahdr.substring(6);
                ahdr = new String(Base64.decodeBase64(ahdr));
                String[] userAndPass = ahdr.split(":");
                if (!USERNAME.equals(userAndPass[0]) || !PASSWORD.equals(userAndPass[1])) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Wrong credentials");
                    return;
                }
                
                response.setStatus(200);
                PrintWriter rw = response.getWriter();
                rw.write("auth: " + ahdr);
                rw.close();
            }
        };
        return new Handler[] { authenticationHandler };
    }
    
    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void noAuthorization() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
    }
    
    /**
     * Tests if the plugin can handle failed authentication
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void invalidCredentials() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(USERNAME + "!");
        t.password(PASSWORD + "!");
        t.execute();
    }

    /**
     * Tests if the plugin doesn't accept invalid authentication type
     * @throws Exception if anything goes wrong
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidAuthType() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.authType(INVALID_AUTHENTICATION_TYPE);
        t.username(USERNAME + "!");
        t.password(PASSWORD + "!");
        t.execute();
    }
    
    /**
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validCredentials() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        t.execute();
        
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("auth: " + USERNAME + ":" + PASSWORD, dstContents);
    }
}

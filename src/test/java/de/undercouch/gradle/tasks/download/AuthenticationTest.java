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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.auth.NTLMScheme;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Before;
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
    private static final String REALM = "Gradle";
    private static final String NONCE = "ABCDEF0123456789";
    
    private boolean basic = true;
    
    /**
     * Set up the test
     * @throws Exception if anything goes wrong
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        basic = true;
    }
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler authenticationHandler = new ContextHandler("/" + AUTHENTICATE) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                String ahdr = request.getHeader("Authorization");
                if (ahdr == null) {
                    if (!basic) {
                        response.setHeader("WWW-Authenticate",
                                "Digest realm=\"" + REALM + "\"," +
                                "nonce=\"" + NONCE + "\"");
                    }
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "No authorization header given");
                    return;
                }
                
                if (basic) {
                    checkBasic(ahdr, response);
                } else {
                    checkDigest(ahdr, response);
                }
            }
            
            private void checkBasic(String ahdr, HttpServletResponse response)
                    throws IOException {
                if (!ahdr.startsWith("Basic ")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Authorization header does not start with 'Basic '");
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
            
            private void checkDigest(String ahdr, HttpServletResponse response)
                    throws IOException {
                if (!ahdr.startsWith("Digest ")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Authorization header does not start with 'Digest '");
                    return;
                }
                
                String expectedResponse = USERNAME + ":" + REALM + ":" + PASSWORD;
                expectedResponse = DigestUtils.md5Hex(expectedResponse);
                
                ahdr = ahdr.substring(7);
                String[] parts = ahdr.split(",");
                for (String p : parts) {
                    if (p.startsWith("username") &&
                            !p.equals("username=\"" + USERNAME + "\"")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Wrong username");
                        return;
                    } else if (p.startsWith("nonce") &&
                            !p.equals("nonce=\"" + NONCE + "\"")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Wrong nonce");
                        return;
                    } else if (p.startsWith("realm") &&
                            !p.equals("realm=\"" + REALM + "\"")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Wrong realm");
                        return;
                    } else if (p.startsWith("response") &&
                            !p.equals("response=\"" + expectedResponse + "\"")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Wrong response");
                        return;
                    }
                }
                
                response.setStatus(200);
                PrintWriter rw = response.getWriter();
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
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validUserAndPass() throws Exception {
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

    /**
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validCredentials() throws Exception {
        Credentials cred = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.credentials(cred);
        t.execute();
        
        assertEquals(cred, t.getCredentials());
        
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("auth: " + USERNAME + ":" + PASSWORD, dstContents);
    }

    /**
     * Tests if the plugin can access a protected resource
     * @throws Exception if anything goes wrong
     */
    @Test
    public void validDigest() throws Exception {
        basic = false;
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        t.authScheme("Digest");
        t.execute();
    }
    
    /**
     * Make sure the plugin rejects an invalid authentication scheme
     * @throws Exception if anything goes wrong
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidAuthSchemeString() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.authScheme("Foobar");
        t.execute();
    }
    
    /**
     * Make sure the plugin rejects an invalid authentication scheme
     * @throws Exception if anything goes wrong
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidAuthSchemeType() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.authScheme(new File(""));
        t.execute();
    }
    
    /**
     * Make sure the plugin rejects an invalid authentication scheme if
     * username and password are set
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void invalidAuthSchemeWithUserAndPass() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(AUTHENTICATE));
        File dst = folder.newFile();
        t.dest(dst);
        t.username(USERNAME);
        t.password(PASSWORD);
        t.authScheme(new NTLMScheme());
        t.execute();
    }
    
    /**
     * Tests if the plugin correctly converts the Basic authentication scheme
     * @throws Exception if anything goes wrong
     */
    @Test
    public void convertBasic() throws Exception {
        Download t = makeProjectAndTask();
        t.authScheme("Basic");
        assertTrue(t.getAuthScheme() instanceof BasicScheme);
    }
    
    /**
     * Tests if the plugin correctly converts the Digest authentication scheme
     * @throws Exception if anything goes wrong
     */
    @Test
    public void convertDigest() throws Exception {
        Download t = makeProjectAndTask();
        t.authScheme("Digest");
        assertTrue(t.getAuthScheme() instanceof DigestScheme);
    }
    
    /**
     * Tests if the plugin correctly converts the credentials
     * @throws Exception if anything goes wrong
     */
    @Test
    public void convertCredentials() throws Exception {
        Download t = makeProjectAndTask();
        t.username(USERNAME);
        t.password(PASSWORD);
        assertEquals(new UsernamePasswordCredentials(USERNAME, PASSWORD),
                t.getCredentials());
    }

    /**
     * Tests if the plugin has no credentials set by default
     * @throws Exception if anything goes wrong
     */
    @Test
    public void noDefaultCredentials() throws Exception {
        Download t = makeProjectAndTask();
        assertNull(t.getCredentials());
    }
    
    /**
     * Tests if the plugin has no authentications scheme set by default
     * @throws Exception if anything goes wrong
     */
    @Test
    public void noDefaultAuthScheme() throws Exception {
        Download t = makeProjectAndTask();
        assertNull(t.getAuthScheme());
    }
}

// Copyright 2013 Michel Kraemer
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.Resource;

import groovy.lang.Closure;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Tests the gradle-download-task plugin
 * @author Michel Kraemer
 */
public class DownloadTaskPluginTest {
    private static final String HEADERS = "headers:";
    private static final String ECHO_HEADERS = "echo-headers";
    private final static String TEST_FILE_NAME = "test.txt";
    private final static String TEST_FILE_NAME2 = "test2.txt";
    
    private static HttpProxyServer proxy;
    private static int proxyPort;
    private static int proxyCounter = 0;
    
    /**
     * A folder for temporary files
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private File parentDir;
    private File projectDir;
    
    private Server server;
    private byte[] contents;
    private byte[] contents2;
    
    /**
     * Find a free socket port
     * @return the number of the free port
     * @throws IOException if an IO error occurred
     */
    private static int findPort() throws IOException {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
    
    /**
     * Runs a proxy server counting requests
     * @throws Exception if the proxy server could not be started
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        proxyPort = findPort();
        proxy = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    public HttpFilters filterRequest(HttpRequest originalRequest,
                            ChannelHandlerContext ctx) {
                       return new HttpFiltersAdapter(originalRequest) {
                          @Override
                          public void proxyToServerRequestSent() {
                              proxyCounter++;
                          }
                       };
                    }
                })
                .start();
    }
    
    /**
     * Stops the proxy server
     */
    @AfterClass
    public static void tearDownClass() {
        proxy.stop();
    }
    
    /**
     * Runs an embedded HTTP server and creates test files to serve
     * @throws Exception if the server could not be started
     */
    @Before
    public void setUp() throws Exception {
        //run server on any free port
        server = new Server(0);
        
        //serve resources from temporary folder
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newResource(
                folder.getRoot().getAbsolutePath()));

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

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler,
                echoHeadersHandler,
                new DefaultHandler() });
        server.setHandler(handlers);
        
        server.start();
        
        //create temporary files
        contents = new byte[4096];
        contents2 = new byte[4096];
        for (int i = 0; i < contents.length; ++i) {
            contents[i] = (byte)(Math.random() * 255);
            contents2[i] = (byte)(Math.random() * 255);
        }
        
        parentDir = folder.newFolder("test");
        projectDir = new File(parentDir, "project");
        
        File testFile = folder.newFile(TEST_FILE_NAME);
        FileUtils.writeByteArrayToFile(testFile, contents);
        File testFile2 = folder.newFile(TEST_FILE_NAME2);
        FileUtils.writeByteArrayToFile(testFile2, contents2);
        
        // reset proxy request counter
        proxyCounter = 0;
    }
    
    /**
     * Stops the embedded HTTP server
     * @throws Exception if the server could not be stopped
     */
    @After
    public void tearDown() throws Exception {
        server.stop();
    }
    
    /**
     * @return the port the embedded HTTP server is listening to
     */
    private int getServerPort() {
        return server.getConnectors()[0].getLocalPort();
    }
    
    /**
     * Makes a Gradle project and creates a download task
     * @return the unconfigured download task
     */
    private Download makeProjectAndTask() {
        Project parent = ProjectBuilder.builder().withProjectDir(parentDir).build();
        Project project = ProjectBuilder.builder().withParent(parent).withProjectDir(projectDir).build();
        
        Map<String, Object> applyParams = new HashMap<String, Object>();
        applyParams.put("plugin", "de.undercouch.download");
        project.apply(applyParams);
        
        Map<String, Object> taskParams = new HashMap<String, Object>();
        taskParams.put("type", Download.class);
        Download t = (Download)project.task(taskParams, "downloadFile");
        return t;
    }
    
    /**
     * Makes a verify task
     * @param downloadTask a configured download task to depend on
     * @return the unconfigured verify task
     */
    private Verify makeVerifyTask(Download downloadTask) {
        Map<String, Object> taskParams = new HashMap<String, Object>();
        taskParams.put("type", Verify.class);
        Verify v = (Verify)downloadTask.getProject().task(taskParams, "verifyFile");
        v.dependsOn(downloadTask);
        return v;
    }
    
    /**
     * Makes a URL for a file provided by the embedded HTTP server
     * @param fileName the file's name
     * @return the URL
     */
    private String makeSrc(String fileName) {
        return "http://localhost:" + getServerPort() + "/" + fileName;
    }
    
    /**
     * Tests if a single file can be downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
        
        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals(contents, dstContents);
    }
    
    /**
     * Tests if a single file can be downloaded from a URL
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleURL() throws Exception {
        Download t = makeProjectAndTask();
        t.src(new URL(makeSrc(TEST_FILE_NAME)));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
        
        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals(contents, dstContents);
    }
    
    /**
     * Tests if a single file can be downloaded to a directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFileToDir() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = folder.newFolder();
        t.dest(dst);
        t.execute();
        
        byte[] dstContents = FileUtils.readFileToByteArray(
                new File(dst, TEST_FILE_NAME));
        assertArrayEquals(contents, dstContents);
    }

    /**
     * Tests if a file is downloaded to the project directory when specifying
     * a relative path
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFileToRelativePath() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.dest(TEST_FILE_NAME);
        t.execute();
        
        byte[] dstContents = FileUtils.readFileToByteArray(
                new File(projectDir, TEST_FILE_NAME));
        assertArrayEquals(contents, dstContents);
    }
    
    /**
     * Tests if a multiple files can be downloaded to a directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(makeSrc(TEST_FILE_NAME), makeSrc(TEST_FILE_NAME2)));
        
        File dst = folder.newFolder();
        t.dest(dst);
        t.execute();
        
        byte[] dstContents = FileUtils.readFileToByteArray(
                new File(dst, TEST_FILE_NAME));
        assertArrayEquals(contents, dstContents);
        byte[] dstContents2 = FileUtils.readFileToByteArray(
                new File(dst, TEST_FILE_NAME2));
        assertArrayEquals(contents2, dstContents2);
    }
    
    /**
     * Tests if the task throws an exception if you try to download
     * multiple files to a single destination file
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void downloadMultipleFilesToFile() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(makeSrc(TEST_FILE_NAME), makeSrc(TEST_FILE_NAME2)));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
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

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals(HEADERS + "\n  X-Header-Test-A: value A\n  "
                + "X-Header-Test-B: value B\n", dstContents);
    }
    
    /**
     * Tests lazy evaluation of 'src' and 'dest' properties
     * @throws Exception if anything goes wrong
     */
    @Test
    public void lazySrcAndDest() throws Exception {
        final boolean[] srcCalled = new boolean[] { false };
        final boolean[] dstCalled = new boolean[] { false };
        
        final File dst = folder.newFile();
        
        Download t = makeProjectAndTask();
        t.src(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -4463658999363261400L;
            
            @SuppressWarnings("unused")
            public Object doCall() {
                srcCalled[0] = true;
                return makeSrc(TEST_FILE_NAME);
            }
        });
        
        t.dest(new Closure<Object>(this, this) {
            private static final long serialVersionUID = 932174549047352157L;

            @SuppressWarnings("unused")
            public Object doCall() {
                dstCalled[0] = true;
                return dst;
            }
        });
        
        t.execute();
        
        assertTrue(srcCalled[0]);
        assertTrue(dstCalled[0]);
        
        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals(contents, dstContents);
    }
    
    /**
     * Tests if the Verify task can verify a file using its MD5 checksum
     * @throws Exception if anything goes wrong
     */
    @Test
    public void verifyMD5() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        String calculatedChecksum = Hex.encodeHexString(md5.digest());
        v.checksum(calculatedChecksum);
        v.src(t.getDest());
        
        t.execute();
        v.execute(); // will throw if the checksum is not OK
    }
    
    /**
     * Tests if the Verify task fails if the checksum is wrong
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void verifyWrongMD5() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        v.checksum("WRONG");
        v.src(t.getDest());
        
        t.execute();
        v.execute(); // should throw
    }
    
    /**
     * Test if the task is skipped if we're in offline mode
     * @throws Exception if anything goes wrong
     */
    @Test
    public void offlineSkip() throws Exception {
        Download t = makeProjectAndTask();
        t.getProject().getGradle().getStartParameter().setOffline(true);
        t.src(makeSrc(TEST_FILE_NAME));
        
        // create empty destination file
        File dst = folder.newFile();
        t.dest(dst);
        
        t.execute();
        
        // file should still be empty
        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals(new byte[0], dstContents);
    }
    
    /**
     * Test if the task fails we're in offline mode and the file does
     * not exist already
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void offlineFail() throws Exception {
        Download t = makeProjectAndTask();
        t.getProject().getGradle().getStartParameter().setOffline(true);
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = new File(folder.getRoot(), "offlineFail");
        t.dest(dst);
        t.execute(); // should fail
    }
    
    /**
     * Tests if a single file can be downloaded through a proxy server
     * @throws Exception if anything goes wrong
     */
    @Test
    public void proxy() throws Exception {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        
        try {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(
                    DownloadTaskPluginTest.proxyPort));
            if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_7)) {
                System.setProperty("http.nonProxyHosts", "");
            } else {
                System.setProperty("http.nonProxyHosts", "~localhost");
            }
            
            Download t = makeProjectAndTask();
            t.src(makeSrc(TEST_FILE_NAME));
            File dst = folder.newFile();
            t.dest(dst);
            t.execute();
            
            byte[] dstContents = FileUtils.readFileToByteArray(dst);
            assertArrayEquals(contents, dstContents);
            assertEquals(1, proxyCounter);
        } finally {
            if (proxyHost == null) {
                System.getProperties().remove("http.proxyHost");
            } else {
                System.setProperty("http.proxyHost", proxyHost);
            }
            if (proxyPort == null) {
                System.getProperties().remove("http.proxyPort");
            } else {
                System.setProperty("http.proxyPort", proxyPort);
            }
            if (nonProxyHosts == null) {
                System.getProperties().remove("http.nonProxyHosts");
            } else {
                System.setProperty("http.nonProxyHosts", nonProxyHosts);
            }
        }
    }
}

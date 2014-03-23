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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.Resource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests the gradle-download-task plugin
 * @author Michel Kraemer
 */
public class DownloadTaskPluginTest {
    private final static String TEST_FILE_NAME = "test.txt";
    private final static String TEST_FILE_NAME2 = "test2.txt";
    
    /**
     * A folder for temporary files
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private Server server;
    private byte[] contents;
    private byte[] contents2;
    
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
        ContextHandler echoHeadersHandler = new ContextHandler("/echo-headers") {
            @Override
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                response.setStatus(200);
                PrintWriter rw = response.getWriter();
                rw.write("headers:\n");
                Enumeration<String> headerNames = request.getHeaderNames();
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
        
        File testFile = folder.newFile(TEST_FILE_NAME);
        FileUtils.writeByteArrayToFile(testFile, contents);
        File testFile2 = folder.newFile(TEST_FILE_NAME2);
        FileUtils.writeByteArrayToFile(testFile2, contents2);
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
        Project project = ProjectBuilder.builder().build();
        
        Map<String, Object> applyParams = new HashMap<String, Object>();
        applyParams.put("plugin", "download-task");
        project.apply(applyParams);
        
        Map<String, Object> taskParams = new HashMap<String, Object>();
        taskParams.put("type", Download.class);
        Download t = (Download)project.task(taskParams, "downloadFile");
        return t;
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
        t.src(makeSrc("echo-headers"));
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("headers:\n", dstContents);
    }

    /**
     * Tests that specified request headers are included
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadWithHeaders() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc("echo-headers"));
        File dst = folder.newFile();
        t.dest(dst);
        t.header("X-Header-Test-A", "value A");
        t.header("X-Header-Test-B", "value B");
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("headers:\n  X-Header-Test-A: value A\n  X-Header-Test-B: value B\n", dstContents);
    }
}

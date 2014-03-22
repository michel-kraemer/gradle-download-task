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

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.Resource;

/**
 * Tests the gradle-download-task plugin
 * @author Michel Kraemer
 */
public class DownloadTaskPluginTest {
    private final static String TEST_FILE_NAME = "test.txt";
    
    /**
     * A folder for temporary files
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private Server server;
    private byte[] contents;
    
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
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler,
                new DefaultHandler() });
        server.setHandler(handlers);
        
        server.start();
        
        //create temporary files
        contents = new byte[4096];
        for (int i = 0; i < contents.length; ++i) {
            contents[i] = (byte)(Math.random() * 255);
        }
        
        File testFile = folder.newFile(TEST_FILE_NAME);
        FileUtils.writeByteArrayToFile(testFile, contents);
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
}

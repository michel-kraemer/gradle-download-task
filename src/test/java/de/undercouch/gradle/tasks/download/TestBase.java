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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.Resource;

/**
 * Base class for unit tests
 * @author Michel Kraemer
 */
public abstract class TestBase {
    /**
     * File name of the first test file
     */
    protected final static String TEST_FILE_NAME = "test.txt";
    
    /**
     * File name of the first test file MD5 checksum file in MD5Sum format
     */
    protected final static String TEST_FILE_NAME_MD5SUM_MD5 = "test.txt.md5sum.md5";
    
    /**
     * File name of the first test file MD5 checksum file in MD5Sum format with bad data
     */
    protected final static String TEST_FILE_NAME_MD5SUM_MD5_BAD = "test.txt.md5sum.md5bad";
    
    /**
     * File name of the first test file MD5 checksum file in GPG MD5 format <file>: <MD5 sum>
     */
    protected final static String TEST_FILE_NAME1_GPGMD5_MD5 = "test1.txt.gpgmd5.md5";
    
    /**
     * File name of the first test file MD5 checksum file in GPG MD5 format <file>: <MD5 sum> with bad data
     */
    protected final static String TEST_FILE_NAME1_GPGMD5_MD5_BAD = "test1.txt.gpgmd5.md5bad";
    
    /**
     * File name of the first test file MD5 checksum file in GPG MD5 format <file>: <hash algo> = <MD5 sum>
     */
    protected final static String TEST_FILE_NAME2_GPGMD5_MD5 = "test2.txt.gpgmd5.md5";
    
    /**
     * File name of the first test file MD5 checksum file in GPG MD5 format <file>: <hash algo> = <MD5 sum> with bad data
     */
    protected final static String TEST_FILE_NAME2_GPGMD5_MD5_BAD = "test2.txt.gpgmd5.md5.bad";
    
    /**
     * File name of the second test file
     */
    protected final static String TEST_FILE_NAME2 = "test2.txt";
    
    /**
     * Parent directory of {@link #projectDir}
     */
    private File parentDir;
    
    /**
     * A temporary directory where a virtual test project is stored
     */
    protected File projectDir;
    
    /**
     * A folder for temporary files
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    /**
     * The HTTP server to test against
     */
    private Server server;
    
    /**
     * Contents of the first test file with the name {@link #TEST_FILE_NAME}
     */
    protected byte[] contents;
    
    /**
     * Contents of the second test file with the name {@link #TEST_FILE_NAME2}
     */
    protected byte[] contents2;
    
    /**
     * @return the HTTP server used for testing
     */
    protected Server createServer() {
        //run server on any free port
        return new Server(0);
    }
    
    /**
     * Runs an embedded HTTP server and creates test files to serve
     * @throws Exception if the server could not be started
     */
    @Before
    public void setUp() throws Exception {
        server = createServer();
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(makeHandlers());
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
		
        File testFileMd5 = folder.newFile(TEST_FILE_NAME_MD5SUM_MD5);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        String calculatedChecksum = Hex.encodeHexString(md5.digest());
        FileUtils.writeStringToFile(testFileMd5, calculatedChecksum+" *"+TEST_FILE_NAME);
		
        testFileMd5 = folder.newFile(TEST_FILE_NAME1_GPGMD5_MD5);
        StringBuilder output = new StringBuilder(TEST_FILE_NAME);
        output.append(": ");
        for (int pos = 0; pos< 16 ; pos+=2) output.append(calculatedChecksum.substring(pos, pos+2)).append(" ");
        output.append(" ");
        for (int pos = 16; pos< 32 ; pos+=2) output.append(calculatedChecksum.substring(pos, pos+2)).append(" ");
        FileUtils.writeStringToFile(testFileMd5, output.toString().trim());
		
        testFileMd5 = folder.newFile(TEST_FILE_NAME2_GPGMD5_MD5);
        output = new StringBuilder(TEST_FILE_NAME);
        output.append(": MD5 = ");
        for (int pos = 0; pos< 16 ; pos+=2) output.append(calculatedChecksum.substring(pos, pos+2)).append(" ");
        output.append(" ");
        for (int pos = 16; pos< 32 ; pos+=2) output.append(calculatedChecksum.substring(pos, pos+2)).append(" ");
        FileUtils.writeStringToFile(testFileMd5, output.toString().trim());
		
        File testFileMd5Bad = folder.newFile(TEST_FILE_NAME_MD5SUM_MD5_BAD);
        FileUtils.writeStringToFile(testFileMd5Bad, "WRONG *"+TEST_FILE_NAME_MD5SUM_MD5_BAD);
		
        File testFile2 = folder.newFile(TEST_FILE_NAME2);
        FileUtils.writeByteArrayToFile(testFile2, contents2);
    }
    
    /**
     * Make the handlers for the HTTP server to test against
     * @return the handlers
     * @throws IOException if the handlers could not be created
     */
    protected Handler[] makeHandlers() throws IOException {
        //serve resources from temporary folder
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newResource(
                folder.getRoot().getAbsolutePath()));
        return new Handler[] { resourceHandler, new DefaultHandler() };
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
     * Find a free socket port
     * @return the number of the free port
     * @throws IOException if an IO error occurred
     */
    protected static int findPort() throws IOException {
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
     * Makes a Gradle project and creates a download task
     * @return the unconfigured download task
     */
    protected Download makeProjectAndTask() {
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
     * @return the port the embedded HTTP server is listening to
     */
    protected int getServerPort() {
        return server.getConnectors()[0].getLocalPort();
    }
    
    /**
     * Makes a URL for a file provided by the embedded HTTP server
     * @param fileName the file's name
     * @return the URL
     */
    protected String makeSrc(String fileName) {
        return "http://localhost:" + getServerPort() + "/" + fileName;
    }
}

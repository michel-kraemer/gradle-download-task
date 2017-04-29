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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;

/**
 * Tests if the plugin uses the ETag header correctly
 * @author Michel Kraemer
 */
public class ETagTest extends TestBase {
    private static final String ETAG = "etag";
    private String etag;
    
    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler lastModifiedHandler = new ContextHandler("/" + ETAG) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                String ifNoneMatch = request.getHeader("If-None-Match");
                if (etag != null && etag.equals(ifNoneMatch)) {
                    response.setStatus(304);
                    response.flushBuffer();
                } else {
                    response.setStatus(200);
                    if (etag != null) {
                        response.setHeader("ETag", etag);
                    }
                    PrintWriter rw = response.getWriter();
                    rw.write("etag: " + etag);
                    rw.close();
                }
            }
        };
        return new Handler[] { lastModifiedHandler };
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        etag = null;
    }
    
    /**
     * Tests if the plugin can handle a missing ETag header
     * @throws Exception if anything goes wrong
     */
    @Test
    public void missingETag() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: null", dstContents);
        assertFalse(t.getCachedETagsFile().exists());
    }
    
    /**
     * Tests if the plugin can handle an incorrect (unquoted) ETag header and
     * still downloads the file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void incorrectETag() throws Exception {
        etag = "abcd";
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: abcd", dstContents);
    }
    
    /**
     * Tests if the plugin downloads a file and stores the etag correctly to
     * the default cached etags file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeMultipleETags() throws Exception {
        String etag1 = "\"foobar1\"";
        String etag2 = "\"foobar2\"";

        //download first file
        etag = etag1;
        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG + "/file1"));
        File dst1 = folder.newFile();
        dst1.delete();
        assertFalse(dst1.exists());
        t.dest(dst1);
        t.onlyIfModified(true);
        t.useETag(true);
        t.execute();

        //download second file
        etag = etag2;
        t = makeProjectAndTask();
        t.src(makeSrc(ETAG + "/file2"));
        File dst2 = folder.newFile();
        dst2.delete();
        assertFalse(dst2.exists());
        t.dest(dst2);
        t.onlyIfModified(true);
        t.useETag(true);
        t.execute();

        //check server responses
        String dst1Contents = FileUtils.readFileToString(dst1);
        assertEquals("etag: " + etag1, dst1Contents);
        String dst2Contents = FileUtils.readFileToString(dst2);
        assertEquals("etag: " + etag2, dst2Contents);

        //read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        //check cached etags
        Map<String, Object> expectedETag1 = new LinkedHashMap<String, Object>();
        expectedETag1.put("ETag", etag1);
        Map<String, Object> expectedETag2 = new LinkedHashMap<String, Object>();
        expectedETag2.put("ETag", etag2);

        Map<String, Object> expectedHost = new LinkedHashMap<String, Object>();
        expectedHost.put("/" + ETAG + "/file1", expectedETag1);
        expectedHost.put("/" + ETAG + "/file2", expectedETag2);

        Map<String, Object> expectedCachedETags = new LinkedHashMap<String, Object>();
        expectedCachedETags.put(this.makeHost(), expectedHost);

        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the plugin downloads two files and stores their etags correctly
     * to the default cached etags file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeETag() throws Exception {
        etag = "\"foobar\"";
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        assertTrue((Boolean)t.getUseETag());
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: " + etag, dstContents);
        
        File buildDir = new File(projectDir, "build");
        File downloadTaskDir = new File(buildDir, "download-task");
        assertEquals(downloadTaskDir, t.getDownloadTaskDir());
        File cachedETagsFile = new File(downloadTaskDir, "etags.json");
        assertEquals(cachedETagsFile, t.getCachedETagsFile());
        
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                cachedETagsFile, "UTF-8");
        
        Map<String, Object> expectedETag = new LinkedHashMap<String, Object>();
        expectedETag.put("ETag", etag);
        
        Map<String, Object> expectedHost = new LinkedHashMap<String, Object>();
        expectedHost.put("/" + ETAG, expectedETag);
        
        Map<String, Object> expectedCachedETags = new LinkedHashMap<String, Object>();
        expectedCachedETags.put(this.makeHost(), expectedHost);
        
        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the downloadTaskDir can be configured
     * @throws Exception if anything goes wrong
     */
    @Test
    public void configureDownloadTaskDir() throws Exception {
        etag = "\"foobar\"";

        Download t = makeProjectAndTask();
        File newDownloadTaskDir = folder.newFolder();
        t.downloadTaskDir(newDownloadTaskDir);
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: " + etag, dstContents);

        assertEquals(newDownloadTaskDir, t.getDownloadTaskDir());
        File cachedETagsFile = new File(newDownloadTaskDir, "etags.json");
        assertTrue(cachedETagsFile.exists());
        assertEquals(cachedETagsFile, t.getCachedETagsFile());
    }

    /**
     * Tests if the cachedETagsFile can be configured
     * @throws Exception if anything goes wrong
     */
    @Test
    public void configureCachedETagsFile() throws Exception {
        etag = "\"foobar\"";

        Download t = makeProjectAndTask();
        File newCachedETagsFile = folder.newFile();
        newCachedETagsFile.delete();
        t.cachedETagsFile(newCachedETagsFile);
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: " + etag, dstContents);
        
        assertEquals(newCachedETagsFile, t.getCachedETagsFile());
        assertTrue(newCachedETagsFile.exists());
    }
    
    /**
     * Tests if the plugin doesn't download a file if the etag equals
     * the cached one
     * @throws Exception if anything goes wrong
     */
    @Test
    public void dontDownloadIfEqual() throws Exception {
        etag = "\"foobar\"";
        
        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        
        //prepare cached etags file
        Map<String, Object> etagMap = new LinkedHashMap<String, Object>();
        etagMap.put("ETag", etag);
        Map<String, Object> hostMap = new LinkedHashMap<String, Object>();
        hostMap.put("/" + ETAG, etagMap);
        Map<String, Object> cachedETags = new LinkedHashMap<String, Object>();
        cachedETags.put(makeHost(), hostMap);
        String cachedETagsContents = JsonOutput.toJson(cachedETags);
        FileUtils.writeStringToFile(t.getCachedETagsFile(), cachedETagsContents);

        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("Hello", dstContents);
    }

    /**
     * Tests if the plugin downloads a file if its ETag does not match the
     * cached one
     * @throws Exception if anything goes wrong
     */
    @Test
    public void modifiedDownload() throws Exception {
        String wrongEtag = "\"barfoo\"";
        etag = "\"foobar\"";

        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);

        //prepare cached etags file
        Map<String, Object> etagMap = new LinkedHashMap<String, Object>();
        etagMap.put("ETag", wrongEtag);
        Map<String, Object> hostMap = new LinkedHashMap<String, Object>();
        hostMap.put("/" + ETAG, etagMap);
        Map<String, Object> cachedETags = new LinkedHashMap<String, Object>();
        cachedETags.put(makeHost(), hostMap);
        String cachedETagsContents = JsonOutput.toJson(cachedETags);
        FileUtils.writeStringToFile(t.getCachedETagsFile(), cachedETagsContents);

        t.execute();

        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: " + etag, dstContents);
    }

    /**
     * Tests if the plugin supports weak etags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeWeakETagIssueWarning() throws Exception {
        etag = "W/\"foobar1\"";

        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag(true);
        assertEquals(Boolean.TRUE, t.getUseETag());
        t.execute();

        //check server response
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: " + etag, dstContents);

        //read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        //check cached etags
        Map<String, Object> expectedETag = new LinkedHashMap<String, Object>();
        expectedETag.put("ETag", etag);
        Map<String, Object> expectedHost = new LinkedHashMap<String, Object>();
        expectedHost.put("/" + ETAG, expectedETag);
        Map<String, Object> expectedCachedETags = new LinkedHashMap<String, Object>();
        expectedCachedETags.put(this.makeHost(), expectedHost);
        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the plugin supports weak etags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeAllETags() throws Exception {
        etag = "W/\"foobar1\"";

        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG));
        File dst = folder.newFile();
        dst.delete();
        assertFalse(dst.exists());
        t.dest(dst);
        t.onlyIfModified(true);
        t.useETag("all");
        assertEquals("all", t.getUseETag());
        t.execute();

        //check server response
        String dstContents = FileUtils.readFileToString(dst);
        assertEquals("etag: " + etag, dstContents);

        //read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        //check cached etags
        Map<String, Object> expectedETag = new LinkedHashMap<String, Object>();
        expectedETag.put("ETag", etag);
        Map<String, Object> expectedHost = new LinkedHashMap<String, Object>();
        expectedHost.put("/" + ETAG, expectedETag);
        Map<String, Object> expectedCachedETags = new LinkedHashMap<String, Object>();
        expectedCachedETags.put(this.makeHost(), expectedHost);
        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Tests if the plugin can ignore weak ETags
     * @throws Exception if anything goes wrong
     */
    @Test
    public void storeStrongOnly() throws Exception {
        String etag1 = "W/\"foobar1\"";
        String etag2 = "\"foobar2\"";

        //download first file
        etag = etag1;
        Download t = makeProjectAndTask();
        t.src(makeSrc(ETAG + "/file1"));
        File dst1 = folder.newFile();
        dst1.delete();
        assertFalse(dst1.exists());
        t.dest(dst1);
        t.onlyIfModified(true);
        t.useETag("strongOnly");
        assertEquals("strongOnly", t.getUseETag());
        t.execute();

        //download second file
        etag = etag2;
        t = makeProjectAndTask();
        t.src(makeSrc(ETAG + "/file2"));
        File dst2 = folder.newFile();
        dst2.delete();
        assertFalse(dst2.exists());
        t.dest(dst2);
        t.onlyIfModified(true);
        t.useETag("strongOnly");
        assertEquals("strongOnly", t.getUseETag());
        t.execute();

        //check server responses
        String dst1Contents = FileUtils.readFileToString(dst1);
        assertEquals("etag: " + etag1, dst1Contents);
        String dst2Contents = FileUtils.readFileToString(dst2);
        assertEquals("etag: " + etag2, dst2Contents);

        //read cached etags file
        JsonSlurper slurper = new JsonSlurper();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedETags = (Map<String, Object>)slurper.parse(
                t.getCachedETagsFile(), "UTF-8");

        //check cached etags (there should be no entry for etag1)
        Map<String, Object> expectedETag2 = new LinkedHashMap<String, Object>();
        expectedETag2.put("ETag", etag2);

        Map<String, Object> expectedHost = new LinkedHashMap<String, Object>();
        expectedHost.put("/" + ETAG + "/file2", expectedETag2);

        Map<String, Object> expectedCachedETags = new LinkedHashMap<String, Object>();
        expectedCachedETags.put(this.makeHost(), expectedHost);

        assertEquals(expectedCachedETags, cachedETags);
    }

    /**
     * Make sure we cannot assign an invalid value to the "useETag" flag
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidUseETagFlag() {
        Download t = makeProjectAndTask();
        t.useETag("foobar");
    }
}

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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Test;

import groovy.lang.Closure;

/**
 * Tests the gradle-download-task plugin
 * @author Michel Kraemer
 */
public class DownloadTest extends TestBase {
    /**
     * Tests if a single file can be downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        Download t = makeProjectAndTask();
        String src = makeSrc(TEST_FILE_NAME);
        t.src(src);
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
        
        assertTrue(t.getSrc() instanceof URL);
        assertEquals(src, t.getSrc().toString());
        assertSame(dst, t.getDest());
        
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
        URL src = new URL(makeSrc(TEST_FILE_NAME));
        t.src(src);
        File dst = folder.newFile();
        t.dest(dst);
        t.execute();
        
        assertSame(src, t.getSrc());
        assertSame(dst, t.getDest());
        
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
     * Tests if multiple files can be downloaded to a directory
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
     * Tests if a destination directory is automatically created if multiple
     * files are downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadMultipleFilesCreatesDestDirAutomatically() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(makeSrc(TEST_FILE_NAME), makeSrc(TEST_FILE_NAME2)));

        File dst = folder.newFolder();
        assertTrue(dst.delete());
        t.dest(dst);
        t.execute();
        assertTrue(dst.isDirectory());
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
     * Do not overwrite an existing file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void skipExisting() throws Exception {
        // write contents to destination file
        File dst = folder.newFile();
        FileUtils.writeStringToFile(dst, "Hello");
        
        Download t = makeProjectAndTask();
        String src = makeSrc(TEST_FILE_NAME);
        t.src(src);
        t.dest(dst);
        t.overwrite(false); // do not overwrite the file
        t.execute();

        // contents must not be changed
        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals("Hello".getBytes(), dstContents);
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is invalid
     * @throws Exception if the test succeeds
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSrc() throws Exception {
        Download t = makeProjectAndTask();
        t.src(new Object());
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is empty
     */
    @Test(expected = TaskExecutionException.class)
    public void testExecuteEmptySrc() {
        Download t = makeProjectAndTask();
        t.execute();
    }

    /**
     * Test if the plugin throws an exception if the 'dest' property is invalid
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDest() {
        Download t = makeProjectAndTask();
        t.dest(new Object());
    }

    /**
     * Test if the plugin throws an exception if the 'dest' property is empty
     * @throws Exception if the test succeeds
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExecuteEmptyDest() throws Exception {
        Download t = makeProjectAndTask();
        String src = makeSrc(TEST_FILE_NAME);
        t.src(src);
        t.execute();
    }

    /**
     * Test if the plugin can handle an array containing one string as source
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testArraySrc() throws Exception {
        Download t = makeProjectAndTask();
        String src = makeSrc(TEST_FILE_NAME);
        t.src(new Object[] { src });
        assertTrue(t.getSrc() instanceof URL);
    }
}

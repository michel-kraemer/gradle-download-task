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
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Test;

/**
 * Test the {@link VerifyAction}
 * @author Michel Kraemer
 */
public class VerifyTest extends TestBase {
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
     * Tests if the Verify task can verify a file using its MD5 checksum file
     * @throws Exception if anything goes wrong
     */
    @Test
    public void verifyMD5FromMD5SumFile() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.src(makeSrc(TEST_FILE_NAME_MD5SUM_MD5));
        File dst = folder.newFolder();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        v.checksumFile(new File(dst, TEST_FILE_NAME_MD5SUM_MD5));
        v.src(new File(dst, TEST_FILE_NAME));
        
        t.execute();
        v.execute(); // will throw if the checksum is not OK
    }
    
    /**
     * Tests if the Verify task can verify a file using its GPG MD5 checksum file format 1
     * @throws Exception if anything goes wrong
     */
    @Test
    public void verifyMD5FromGPGFileFormat1() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.src(makeSrc(TEST_FILE_NAME1_GPGMD5_MD5));
        File dst = folder.newFolder();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        v.checksumFile(new File(dst, TEST_FILE_NAME1_GPGMD5_MD5));
        v.src(new File(dst, TEST_FILE_NAME));
        
        t.execute();
        v.execute(); // will throw if the checksum is not OK
    }
    
    /**
     * Tests if the Verify task can verify a file using its GPG MD5 checksum file format 2
     * @throws Exception if anything goes wrong
     */
    @Test
    public void verifyMD5FromGPGFileFormat2() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.src(makeSrc(TEST_FILE_NAME2_GPGMD5_MD5));
        File dst = folder.newFolder();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        v.checksumFile(new File(dst, TEST_FILE_NAME2_GPGMD5_MD5));
        v.src(new File(dst, TEST_FILE_NAME));
        
        t.execute();
        v.execute(); // will throw if the checksum is not OK
    }
    
    /**
     * Tests if the Verify task fails if the checksum in file is wrong
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void verifyWrongMD5File() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.src(makeSrc(TEST_FILE_NAME_MD5SUM_MD5_BAD));
        File dst = folder.newFolder();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        v.checksumFile(new File(dst, TEST_FILE_NAME_MD5SUM_MD5_BAD));
        v.src(new File(dst, TEST_FILE_NAME));
        
        t.execute();
        v.execute(); // should throw
    }
    
    /**
     * Tests if the Verify task fails if no checksum specified
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void verifyNoChecksumSpecified() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.src(makeSrc(TEST_FILE_NAME_MD5SUM_MD5_BAD));
        File dst = folder.newFolder();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        v.src(new File(dst, TEST_FILE_NAME));
        
        t.execute();
        v.execute(); // should throw
    }
    
    /**
     * Tests if the Verify task fails if bot checksums specified
     * @throws Exception if anything goes wrong
     */
    @Test(expected = TaskExecutionException.class)
    public void verifyBothChecksumsSpecified() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.src(makeSrc(TEST_FILE_NAME_MD5SUM_MD5_BAD));
        File dst = folder.newFolder();
        t.dest(dst);
        
        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(contents);
        String calculatedChecksum = Hex.encodeHexString(md5.digest());
        v.checksumFile(new File(dst, TEST_FILE_NAME_MD5SUM_MD5_BAD));
        v.checksum(calculatedChecksum);
        v.src(new File(dst, TEST_FILE_NAME));
        
        t.execute();
        v.execute(); // should throw
    }
    
    /**
     * Tests checksum set/get
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testChecksumSetGet() throws Exception {
        Download t = makeProjectAndTask();
        Verify v = makeVerifyTask(t);
        v.checksum("TEST");
        assertEquals("TEST", v.getChecksum());
    }
    
    /**
     * Tests checksumFile set/get
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testChecksumFileSetGet() throws Exception {
        Download t = makeProjectAndTask();
        Verify v = makeVerifyTask(t);
        
        File dst = folder.newFolder();
        File md5File = new File(dst, TEST_FILE_NAME_MD5SUM_MD5);
        v.checksumFile(md5File);
        assertEquals(md5File, v.getChecksumFile());
    }
    
    /**
     * Tests checksumFile set/get from String
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testChecksumFileSetAsString() throws Exception {
        Download t = makeProjectAndTask();
        Verify v = makeVerifyTask(t);
        
        File dst = folder.newFolder();
        File md5File = new File(dst, TEST_FILE_NAME_MD5SUM_MD5);
        v.checksumFile(md5File.getName());
        assertEquals(TEST_FILE_NAME_MD5SUM_MD5, v.getChecksumFile().getCanonicalFile().getName());
    }
}

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

import static org.junit.Assert.assertArrayEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.junit.Test;

/**
 * Test the offline capabilities of the download task plugin
 * @author Michel Kraemer
 */
public class OfflineTest extends TestBase {
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
    @Test(expected = GradleException.class)
    public void offlineFail() throws Exception {
        Download t = makeProjectAndTask();
        t.getProject().getGradle().getStartParameter().setOffline(true);
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = new File(folder.getRoot(), "offlineFail");
        t.dest(dst);
        t.execute(); // should fail
    }
}

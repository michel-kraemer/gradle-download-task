// Copyright 2013-2017 Michel Kraemer
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

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.junit.Test;

import groovy.lang.Closure;

/**
 * Tests {@link DownloadExtension}
 * @author Michel Kraemer
 */
public class DownloadExtensionTest extends TestBase {
    /**
     * Download a file using the {@link DownloadExtension}
     * @param project a Gradle project
     * @param src the file to download
     * @param dst the download destination
     */
    private void doDownload(Project project, final String src, final File dst) {
        DownloadExtension e = new DownloadExtension(project);
        e.configure(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -7729300978830802384L;

            @SuppressWarnings("unused")
            public void doCall() throws Exception {
                DownloadAction action = (DownloadAction)this.getDelegate();
                action.src(src);
                assertTrue(action.getSrc() instanceof URL);
                assertEquals(src, action.getSrc().toString());
                action.dest(dst);
                assertSame(dst, action.getDest());
            }
        });
    }

    /**
     * Tests if a single file can be downloaded
     * @throws Exception if anything goes wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        Download t = makeProjectAndTask();

        String src = makeSrc(TEST_FILE_NAME);
        File dst = folder.newFile();

        doDownload(t.getProject(), src, dst);

        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals(contents, dstContents);
    }

    /**
     * Tests if the download fails if the file does not exist
     * @throws Exception if anything goes wrong
     */
    @Test(expected = IllegalStateException.class)
    public void downloadSingleFileError() throws Exception {
        Download t = makeProjectAndTask();
        String src = makeSrc("foobar.txt");
        File dst = folder.newFile();
        doDownload(t.getProject(), src, dst);
    }
}

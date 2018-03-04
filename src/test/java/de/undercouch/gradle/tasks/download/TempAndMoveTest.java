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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if a file can be downloaded to a temporary location and then
 * moved to the final location
 * @author Michel Kraemer
 */
public class TempAndMoveTest extends TestBase {
    private static final String TEMPANDMOVE = "tempandmove";
    private File dst;
    private File downloadTaskDir;

    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler tempAndMoveHandler = new ContextHandler("/" + TEMPANDMOVE) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                //at the beginning there should be no dest file and no temp file
                File tempFile = new File(downloadTaskDir, dst.getName() + ".part");
                assertFalse(dst.exists());
                assertFalse(tempFile.exists());

                response.setStatus(200);
                OutputStream os = response.getOutputStream();
                os.write(contents);

                //flush output stream - the plugin should now write to
                //the temporary file
                os.flush();

                //wait for temporary file (max. 10 seconds)
                boolean exists = false;
                for (int i = 0; i < 100; ++i) {
                    exists = tempFile.exists();
                    if (exists) {
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                //temp file should now exist, but dest file not
                assertFalse(dst.exists());
                assertTrue(exists);

                os.close();
            }
        };
        return new Handler[] { tempAndMoveHandler };
    }

    /**
     * Tests if a file can be downloaded to a temporary location and then
     * moved to the final location
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void tempAndMove() throws Exception {
        Download t = makeProjectAndTask();
        downloadTaskDir = t.getDownloadTaskDir();
        String src = makeSrc(TEST_FILE_NAME);
        t.src(src);
        dst = folder.newFile();
        dst.delete(); //make sure dest does not exist, so we can verify correctly
        t.dest(dst);
        t.tempAndMove(true);
        t.execute();

        assertTrue(dst.exists());
        assertFalse(new File(downloadTaskDir, dst.getName() + ".part").exists());

        byte[] dstContents = FileUtils.readFileToByteArray(dst);
        assertArrayEquals(contents, dstContents);
    }
}

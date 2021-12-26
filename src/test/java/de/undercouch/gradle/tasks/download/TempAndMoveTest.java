// Copyright 2013-2019 Michel Kraemer
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.trafficlistener.DoNothingWiremockNetworkTrafficListener;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests if a file can be downloaded to a temporary location and then
 * moved to the final location
 * @author Michel Kraemer
 */
public class TempAndMoveTest extends TestBase {
    private static final String TEMPANDMOVE = "tempandmove";
    private static final String CONTENTS = "Hello world";
    private File dst;
    private File downloadTaskDir;
    private boolean checkDstDoesNotExist;

    /**
     * Run a mock HTTP server with a network listener
     */
    public WireMockServer tempAndMoveWireMock;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        tempAndMoveWireMock = new WireMockServer(options()
                .dynamicPort()
                .networkTrafficListener(new TempAndMoveNetworkTrafficListener())
                .jettyStopTimeout(10000L));
        tempAndMoveWireMock.start();
    }

    @After
    public void tearDown() {
        tempAndMoveWireMock.stop();
    }

    private class TempAndMoveNetworkTrafficListener extends
            DoNothingWiremockNetworkTrafficListener {
        @Override
        public void opened(Socket socket) {
            // at the beginning there should be no dest file and no temp file
            if (checkDstDoesNotExist) {
                assertFalse(dst.exists());
            }
            assertNull(getTempFile());
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer bytes) {
            // wait for temporary file (max. 10 seconds)
            File tempFile = null;
            for (int i = 0; i < 100; ++i) {
                tempFile = getTempFile();
                if (tempFile != null) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // temp file should now exist, but dest file not
            if (checkDstDoesNotExist) {
                assertFalse(dst.exists());
            }
            assertNotNull(tempFile);
        }
    }

    private File getTempFile() {
        File[] files = downloadTaskDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(dst.getName()) && name.endsWith(".part");
            }
        });

        if (files == null) {
            // downloadTaskDir does not exist
            return null;
        }
        if (files.length > 1) {
            fail("Multiple temp files in " + downloadTaskDir);
        }

        return files.length == 1 ? files[0] : null;
    }

    private void testTempAndMove(boolean createDst) throws Exception {
        tempAndMoveWireMock.stubFor(get(urlEqualTo("/" + TEMPANDMOVE))
                .willReturn(aResponse()
                        .withBody(CONTENTS)));

        Download t = makeProjectAndTask();
        downloadTaskDir = t.getDownloadTaskDir();
        String src = tempAndMoveWireMock.url(TEMPANDMOVE);
        t.src(src);
        dst = folder.newFile();

        checkDstDoesNotExist = !createDst;

        if (createDst) {
            dst = folder.newFile();
            OutputStream os = new FileOutputStream(dst);
            os.close();
        } else {
            // make sure dest does not exist, so we can verify correctly
            assertTrue(dst.delete());
        }

        t.dest(dst);
        t.tempAndMove(true);
        t.execute();

        assertTrue(dst.exists());
        assertNull(getTempFile());

        String dstContents = FileUtils.readFileToString(dst,
                StandardCharsets.UTF_8);
        assertEquals(CONTENTS, dstContents);
    }

    /**
     * Tests if a file can be downloaded to a temporary location and then
     * moved to the final location
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void tempAndMove() throws Exception {
        testTempAndMove(false);
    }

    /**
     * Tests if a file can be downloaded to a temporary location and then
     * moved to the final location overwriting existing file
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void tempAndMoveOverwrite() throws Exception {
        testTempAndMove(true);
    }
}

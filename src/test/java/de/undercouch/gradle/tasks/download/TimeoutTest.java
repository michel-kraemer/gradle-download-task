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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gradle.api.UncheckedIOException;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;

/**
 * Tests if the task times out if the response takes too long
 * @author Michel Kraemer
 */
public class TimeoutTest extends TestBase {
    private static final int TIMEOUT_MS = 100;
    private static final String TIMEOUT = "timeout";

    @Override
    protected Handler[] makeHandlers() throws IOException {
        ContextHandler echoHeadersHandler = new ContextHandler("/" + TIMEOUT) {
            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                            throws IOException, ServletException {
                // wait longer than the configured timeout
                try {
                    Thread.sleep(TIMEOUT_MS * 10);
                } catch (InterruptedException e) {
                    // fall through
                }
            }
        };
        return new Handler[] { echoHeadersHandler };
    }

    /**
     * Tests that the task times out if the response takes too long
     * @throws Exception if anything else goes wrong
     */
    @Test
    public void timeout() throws Exception {
        Download t = makeProjectAndTask();
        t.timeout(TIMEOUT_MS);
        assertEquals(TIMEOUT_MS, t.getTimeout());
        t.src(makeSrc(TIMEOUT));
        File dst = folder.newFile();
        t.dest(dst);
        try {
            t.execute();
            fail("Connection should have timed out by now");
        } catch (TaskExecutionException e) {
            assertTrue(e.getCause() instanceof UncheckedIOException);
            assertTrue(e.getCause().getCause() instanceof SocketTimeoutException);
        }
    }
}

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

import org.apache.hc.client5.http.ClientProtocolException;
import org.gradle.workers.WorkerExecutionException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test the offline capabilities of the download task plugin
 * @author Michel Kraemer
 */
public class OfflineTest extends TestBaseWithMockServer {
    /**
     * Test if the task is skipped if we're in offline mode
     * @throws Exception if anything goes wrong
     */
    @Test
    public void offlineSkip() throws Exception {
        Download t = makeOfflineProjectAndTask();
        t.src(wireMock.baseUrl());

        // create empty destination file
        File dst = newTempFile();
        t.dest(dst);

        execute(t);

        // file should still be empty
        assertThat(dst).isEmpty();
    }

    /**
     * Test if the task fails if we're in offline mode and the file does
     * not exist already
     */
    @Test
    public void offlineFail() {
        Download t = makeProjectAndTask();
        t.getProject().getGradle().getStartParameter().setOffline(true);
        t.src(wireMock.baseUrl());
        File dst = new File(folder.toFile(), "offlineFail");
        t.dest(dst);
        assertThatThrownBy(() -> execute(t))
                .isInstanceOf(WorkerExecutionException.class)
                .hasRootCauseInstanceOf(ClientProtocolException.class);
    }

    /**
     * Creates a Download task and configures it to run in offline mode
     * @return the task
     */
    private Download makeOfflineProjectAndTask() {
        return makeProjectAndTask(project -> project.getGradle()
                .getStartParameter().setOffline(true));
    }
}

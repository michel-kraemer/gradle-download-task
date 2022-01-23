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

import groovy.lang.Closure;
import kotlin.jvm.functions.Function0;
import org.apache.commons.codec.binary.Hex;
import org.gradle.api.GradleException;
import org.gradle.api.internal.provider.DefaultProvider;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test the {@link VerifyAction}
 * @author Michel Kraemer
 */
public class VerifyTest extends TestBaseWithMockServer {
    /**
     * Calculates the MD5 checksum for {@link #CONTENTS}
     * @return the checksum
     */
    private String calculateChecksum() {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md5.update(CONTENTS.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(md5.digest());
    }

    /**
     * Makes a verify task
     * @param downloadTask a configured download task to depend on
     * @return the unconfigured verify task
     */
    private Verify makeVerifyTask(Download downloadTask) {
        Map<String, Object> taskParams = new HashMap<>();
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
        configureDefaultStub();

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        assertThat(v.getAlgorithm()).isEqualTo("MD5");
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);
        assertThat(v.getChecksum()).isEqualTo(calculatedChecksum);
        v.src(t.getDest());
        assertThat(v.getSrc()).isEqualTo(t.getDest());

        execute(t);
        execute(v); // will throw if the checksum is not OK
    }

    /**
     * Tests if the Verify task fails if the checksum is wrong
     * @throws Exception if anything goes wrong
     */
    @Test
    public void verifyWrongMD5() throws Exception {
        configureDefaultStub();

        Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        v.checksum("WRONG");
        v.src(t.getDest());

        execute(t);
        assertThatThrownBy(() -> execute(v))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Invalid checksum for file");
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is empty
     */
    @Test
    public void testExecuteEmptySrc() {
        Download t = makeProjectAndTask();

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);

        assertThatThrownBy(() -> execute(v))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please provide a file to verify");
    }

    /**
     * Test if the plugin throws an exception if the 'algorithm' property is empty
     * @throws Exception if the test succeeds
     */
    @Test
    public void testExecuteEmptyAlgorithm() throws Exception {
        Download t = makeProjectAndTask();
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);
        v.algorithm(null);
        v.src(t.getDest());

        assertThatThrownBy(() -> execute(v))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Please provide the algorithm");
    }

    /**
     * Test if the plugin throws an exception if the 'checksum' property is empty
     * @throws Exception if the test succeeds
     */
    @Test
    public void testExecuteEmptyChecksum() throws Exception {
        Download t = makeProjectAndTask();
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        v.src(t.getDest());

        assertThatThrownBy(() -> execute(v))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Please provide a checksum");
    }

    /**
     * Test if the plugin throws an exception if the 'src' property is invalid
     * @throws Exception if the test succeeds
     */
    @Test
    public void testInvalidSrc() throws Exception {
        Download t = makeProjectAndTask();
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);
        v.algorithm("MD5");

        assertThatThrownBy(() -> v.src(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Verification source must either be");
    }

    /**
     * Tests lazy evaluation of the 'src' property
     * @throws Exception if anything goes wrong
     */
    @Test
    public void lazySrc() throws Exception {
        configureDefaultStub();

        final boolean[] srcCalled = new boolean[] { false };

        final Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);
        v.src(new Closure<Object>(this, this) {
            private static final long serialVersionUID = -53893707548824180L;

            @SuppressWarnings("unused")
            public Object doCall() {
                srcCalled[0] = true;
                return t.getDest().getAbsolutePath();
            }
        });

        execute(t);
        execute(v); // will throw if the checksum is not OK

        assertThat(srcCalled[0]).isTrue();
    }

    /**
     * Tests lazy evaluation of the 'src' property if it is a Provider
     * @throws Exception if anything goes wrong
     */
    @Test
    public void providerSrc() throws Exception {
        configureDefaultStub();

        final boolean[] srcCalled = new boolean[] { false };

        final Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);
        v.src(new DefaultProvider<>((Callable<Object>)() -> {
            srcCalled[0] = true;
            return t.getDest().getAbsolutePath();
        }));

        execute(t);
        execute(v); // will throw if the checksum is not OK

        assertThat(srcCalled[0]).isTrue();
    }

    /**
     * Tests lazy evaluation of the 'src' property if it is a Kotlin Function
     * @throws Exception if anything goes wrong
     */
    @Test
    public void kotlinFunctionSrc() throws Exception {
        configureDefaultStub();

        final boolean[] srcCalled = new boolean[] { false };

        final Download t = makeProjectAndTask();
        t.src(wireMock.url(TEST_FILE_NAME));
        File dst = newTempFile();
        t.dest(dst);

        Verify v = makeVerifyTask(t);
        v.algorithm("MD5");
        String calculatedChecksum = calculateChecksum();
        v.checksum(calculatedChecksum);
        v.src((Function0<Object>)() -> {
            srcCalled[0] = true;
            return t.getDest().getAbsolutePath();
        });

        execute(t);
        execute(v); // will throw if the checksum is not OK

        assertThat(srcCalled[0]).isTrue();
    }
}

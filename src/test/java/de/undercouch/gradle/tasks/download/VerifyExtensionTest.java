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

//import groovy.lang.Closure;
//import org.gradle.api.GradleException;
//import org.gradle.api.Project;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.PrintWriter;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * Tests {@link VerifyExtension}
// * @author Michel Kraemer
// */
//public class VerifyExtensionTest extends TestBase {
//    private static final String EXPECTED_CHECKSUM =
//            "1586cffafa39e38959477da9eaa41c31";
//
//    /**
//     * Verify a file's checksum using the {@link VerifyExtension}
//     * @param project a Gradle project
//     * @param src the file to verify
//     * @param checksum the expected checksum
//     */
//    private void doVerify(Project project, final String src,
//            final String checksum) {
//        VerifyExtension e = new VerifyExtension(project);
//        e.configure(new Closure<Object>(this, this) {
//            private static final long serialVersionUID = 8101704138005375213L;
//
//            @SuppressWarnings("unused")
//            public void doCall() {
//                VerifyAction action = (VerifyAction)this.getDelegate();
//                action.src(src);
//                action.checksum(checksum);
//                assertEquals(checksum, action.getChecksum());
//                assertEquals(src, action.getSrc().toString());
//            }
//        });
//    }
//
//    /**
//     * Create a test file to be verified
//     * @return the test file
//     * @throws IOException if the file could not be created
//     */
//    private File makeSourceFile() throws IOException {
//        File dst = folder.newFile();
//        try (OutputStream os = new FileOutputStream(dst);
//              PrintWriter pw = new PrintWriter(os)) {
//            pw.write("THIS IS A TEST");
//        }
//        return dst;
//    }
//
//    /**
//     * Tests if the checksum of a file can be verified
//     * @throws Exception if anything goes wrong
//     */
//    @Test
//    public void verifyFile() throws Exception {
//        File src = makeSourceFile();
//        Download t = makeProjectAndTask();
//        doVerify(t.getProject(), src.getAbsolutePath(), EXPECTED_CHECKSUM);
//    }
//
//    /**
//     * Tests if the extension fails if the checksum is wrong
//     * @throws Exception if anything goes wrong
//     */
//    @Test(expected = GradleException.class)
//    public void verifyFileChecksumError() throws Exception {
//        File src = makeSourceFile();
//        Download t = makeProjectAndTask();
//        doVerify(t.getProject(), src.getAbsolutePath(), "wrong checksum");
//    }
//
//    /**
//     * Tests if the download fails if the file does not exist
//     * @throws Exception if anything goes wrong
//     */
//    @Test(expected = IllegalStateException.class)
//    public void verifyFileError() throws Exception {
//        File src = makeSourceFile();
//        assertTrue(src.delete());
//        Download t = makeProjectAndTask();
//        doVerify(t.getProject(), src.getAbsolutePath(), EXPECTED_CHECKSUM);
//    }
//}

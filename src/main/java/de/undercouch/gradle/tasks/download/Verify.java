// Copyright 2015 Michel Kraemer
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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * Verifies a file's integrity by calculating its checksum.
 * <pre>
 * task verifyFile(type: Verify) {
 *     file new File(buildDir, "myfile.txt")
 *     algorithm 'MD5'
 *     checksum '694B2863621FCDBBBA2777BF329C056C' // expected checksum (hex)
 * }
 * </pre>
 * @author Michel Kraemer
 */
public class Verify extends DefaultTask implements VerifySpec {
    private final VerifyAction action;

    /**
     * Default constructor
     */
    public Verify() {
        action = new VerifyAction(getProject());
    }
    
    /**
     * Starts verifying
     * @throws IOException if the file could not be verified
     * @throws NoSuchAlgorithmException if the given algorithm is not available
     */
    @TaskAction
    public void verify() throws IOException, NoSuchAlgorithmException {
        action.execute();
    }

    @Override
    public void src(Object src) {
        action.src(src);
    }

    @Override
    public void algorithm(String algorithm) {
        action.algorithm(algorithm);
    }

    @Override
    public void checksum(String checksum) {
        action.checksum(checksum);
    }

    @InputFile
    @Override
    public File getSrc() {
        return action.getSrc();
    }

    @Input
    @Optional
    @Override
    public String getAlgorithm() {
        return action.getAlgorithm();
    }

    @Input
    @Override
    public String getChecksum() {
        return action.getChecksum();
    }
}

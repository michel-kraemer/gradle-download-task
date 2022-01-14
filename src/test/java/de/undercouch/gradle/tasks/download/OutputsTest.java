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

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests related to plugin outputs
 * @author Jan Berkel
 */
public class OutputsTest extends TestBaseWithMockServer {
    /**
     * Test if the outputs are empty if no sources are set
     */
    @Test
    public void emptyOutputs() {
        Download t = makeProjectAndTask();
        assertThat(t.getOutputs().getFiles()).isEmpty();
    }

    /**
     * Test if the output is generated correctly for a single source and a
     * destination file
     * @throws Exception if anything went wrong
     */
    @Test
    public void singleOutputFileWithDestinationFile()
            throws Exception {
        Download t = makeProjectAndTask();
        t.src(wireMock.baseUrl());
        File dst = newTempFile();
        t.dest(dst);
        assertThat(t.getOutputs().getFiles().getSingleFile()).isEqualTo(dst);
    }

    /**
     * Test if the output is generated correctly for a single source and if
     * the destination is the build directory
     */
    @Test
    public void singleOutputFileWithDestinationAsBuildDir1() {
        Download t = makeProjectAndTask();
        t.src(wireMock.baseUrl());
        File buildDir = t.getProject().getBuildDir();
        t.dest(buildDir);
        assertThat(t.getOutputs().getFiles().getSingleFile()).isEqualTo(buildDir);
    }

    /**
     * Test if the output is generated correctly for a single source and if
     * the destination is the build directory (using the ProjectLayout API)
     * as a File
     */
    @Test
    public void singleOutputFileWithDestinationAsBuildDir2() {
        Download t = makeProjectAndTask();
        t.src(wireMock.baseUrl());
        File buildDir = t.getProject().getLayout().getBuildDirectory().getAsFile().get();
        t.dest(buildDir);
        assertThat(t.getOutputs().getFiles().getSingleFile()).isEqualTo(buildDir);
    }

    /**
     * Test if the output is generated correctly for a single source and if
     * the destination is the build directory (using the ProjectLayout API)
     */
    @Test
    public void singleOutputFileWithDestinationAsBuildDirProperty() {
        Download t = makeProjectAndTask();
        t.src(wireMock.baseUrl());
        DirectoryProperty buildDir = t.getProject().getLayout().getBuildDirectory();
        t.dest(buildDir);
        assertThat(t.getOutputs().getFiles().getSingleFile().getParentFile())
                .isEqualTo(buildDir.getAsFile().get());
    }

    /**
     * Test if the output is generated correctly for a single source and if
     * the destination is a valid subdirectory (using the ProjectLayout API)
     */
    @Test
    public void singleOutputFileWithDestinationAsProviderDirectory() {
        Download t = makeProjectAndTask();
        t.src(wireMock.baseUrl());
        Provider<Directory> dir = t.getProject().getLayout().getBuildDirectory()
                .dir("download");
        t.dest(dir);
        // check the output files parent is our dir
        assertThat(t.getOutputs().getFiles().getSingleFile().getParentFile())
                .isEqualTo(dir.get().getAsFile());
    }

    /**
     * Test if the output is generated correctly for a single source and if
     * the destination is a valid RegularFile (using the ProjectLayout API)
     */
    @Test
    public void singleOutputFileWithDestinationAsProviderRegularFile() {
        Download t = makeProjectAndTask();
        t.src(wireMock.baseUrl());
        Provider<RegularFile> file = t.getProject().getLayout()
                .getBuildDirectory().file("exampledownload");
        t.dest(file); // test if dest is build dir
        assertThat(t.getOutputs().getFiles().getSingleFile())
                .isEqualTo(file.get().getAsFile());
    }

    /**
     * Test if the output is generated correctly for a single source and a
     * destination folder
     */
    @Test
    public void singleOutputFileWithDestinationDirectory() {
        Download t = makeProjectAndTask();
        t.src(wireMock.url("test1.txt"));
        t.dest(folder.toFile());
        assertThat(t.getOutputs().getFiles().getSingleFile())
                .isEqualTo(new File(folder.toFile(), "test1.txt"));
    }

    /**
     * Test if the outputs are generated correctly for multiple sources and
     * a destination folder
     */
    @Test
    public void multipleOutputFiles() {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(wireMock.url("test1.txt"), wireMock.url("test2.txt")));
        t.dest(folder.toFile());
        final FileCollection fileCollection = t.getOutputs().getFiles();
        assertThat(fileCollection.getFiles()).containsExactly(
                new File(folder.toFile(), "test1.txt"),
                new File(folder.toFile(), "test2.txt"));
    }
}

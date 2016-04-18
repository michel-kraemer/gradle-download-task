package de.undercouch.gradle.tasks.download;

import org.gradle.api.file.FileCollection;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OutputsTest extends TestBase {

    @Test
    public void emptyOutputs() throws Exception {
        Download t = makeProjectAndTask();
        assertTrue(t.getOutputs().getFiles().isEmpty());
    }

    @Test
    public void singleOutputFileWithDestinationFile() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        File dst = folder.newFile();
        t.dest(dst);
        assertEquals(dst, t.getOutputs().getFiles().getSingleFile());
    }

    @Test
    public void singleOutputFileWithDestinationDirectory() throws Exception {
        Download t = makeProjectAndTask();
        t.src(makeSrc(TEST_FILE_NAME));
        t.dest(folder.getRoot());
        assertEquals(new File(folder.getRoot(), TEST_FILE_NAME), t.getOutputs().getFiles().getSingleFile());
    }

    @Test
    public void multipleOutputFiles() throws Exception {
        Download t = makeProjectAndTask();
        t.src(Arrays.asList(makeSrc(TEST_FILE_NAME), makeSrc(TEST_FILE_NAME2)));
        t.dest(folder.getRoot());
        final FileCollection fileCollection = t.getOutputs().getFiles();
        assertEquals(2, fileCollection.getFiles().size());
        assertTrue(fileCollection.contains(new File(folder.getRoot(), TEST_FILE_NAME)));
        assertTrue(fileCollection.contains(new File(folder.getRoot(), TEST_FILE_NAME2)));
    }
}

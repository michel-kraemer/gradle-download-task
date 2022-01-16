package de.undercouch.gradle.tasks.download.internal;

import java.io.IOException;

/**
 * An asynchronous job executed by a {@link WorkerExecutorHelper}
 * @author Michel Kraemer
 */
public interface Job {
    /**
     * Execute the job
     * @throws IOException if the job failed
     */
    void run() throws IOException;
}

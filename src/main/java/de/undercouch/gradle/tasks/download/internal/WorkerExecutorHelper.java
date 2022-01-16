package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.model.ObjectFactory;
import org.gradle.util.GradleVersion;

/**
 * Executes jobs asynchronously. Either uses the Gradle Worker API (if
 * available) or falls back to a legacy implementation using an
 * {@link java.util.concurrent.ExecutorService}.
 * @author Michel Kraemer
 */
public abstract class WorkerExecutorHelper {
    /**
     * Creates a new instance of the {@link WorkerExecutorHelper} depending
     * on the Gradle version
     * @param objectFactory creates Gradle model objects
     * @return the helper
     */
    public static WorkerExecutorHelper newInstance(ObjectFactory objectFactory) {
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("5.6")) >= 0) {
            return objectFactory.newInstance(DefaultWorkerExecutorHelper.class);
        }
        return new LegacyWorkerExecutorHelper();
    }

    /**
     * Execute a job asynchronously
     * @param job the job to execute
     */
    public abstract void submit(Job job);

    /**
     * Wait for all jobs of the current build operation to complete
     */
    public abstract void await();

    /**
     * Returns {@code true} if {@link #await()} MUST be called at the end of
     * the task. This mostly applies to Gradle versions that don't have a
     * Worker API and therefore cannot let the task continue to run in parallel
     * to others.
     * @return {@code true} if {@link #await()} must be called
     */
    public abstract boolean needsAwait();
}

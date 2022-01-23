package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.UncheckedIOException;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link WorkerExecutorHelper} that executes
 * {@link Job}s asynchronously using the Gradle Worker API
 * @author Michel Kraemer
 */
public class DefaultWorkerExecutorHelper extends WorkerExecutorHelper {
    /**
     * A unique ID for jobs. Used to access jobs in {@link #jobs}
     */
    private static final AtomicInteger UNIQUE_ID = new AtomicInteger();

    /**
     * A maps of jobs submitted to this executor
     */
    private static final Map<Integer, Job> jobs = new ConcurrentHashMap<>();

    private final WorkerExecutor workerExecutor;
    private final WorkQueue workQueue;

    /**
     * Constructs a new executor
     * @param workerExecutor the Gradle Worker API executor
     */
    @Inject
    public DefaultWorkerExecutorHelper(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
        this.workQueue = workerExecutor.noIsolation();
    }

    @Override
    public void submit(Job job) {
        int id = UNIQUE_ID.getAndIncrement();
        jobs.put(id, job);
        workQueue.submit(DefaultWorkAction.class, parameters ->
                parameters.getID().set(id));
    }

    @Override
    public void await() {
        workerExecutor.await();
    }

    @Override
    public boolean needsAwait() {
        return false;
    }

    public interface DefaultWorkParameters extends WorkParameters {
        Property<Integer> getID();
    }

    public static abstract class DefaultWorkAction implements WorkAction<DefaultWorkParameters> {
        @Override
        public void execute() {
            Job job = jobs.remove(getParameters().getID().get());
            try {
                job.run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}

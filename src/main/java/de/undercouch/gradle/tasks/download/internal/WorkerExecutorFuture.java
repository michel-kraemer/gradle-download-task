package de.undercouch.gradle.tasks.download.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A custom implementation of {@link CompletableFuture} that calls
 * {@link WorkerExecutorHelper#await()} on every call to {@link #get()} or
 * {@link #get(long, TimeUnit)}
 */
public class WorkerExecutorFuture extends CompletableFuture<Void> {
    private final WorkerExecutorHelper workerExecutor;

    public WorkerExecutorFuture(WorkerExecutorHelper workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        workerExecutor.await();
        return super.get();
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws ExecutionException,
            InterruptedException, TimeoutException {
        workerExecutor.await();
        return super.get(timeout, unit);
    }

    // TODO As soon as we are on Java 9, we should override this method and return our custom implementation.
    // TODO Otherwise, internal methods of CompletableFuture (such as thenRun) will replace our instance.
    // @Override
    // public <U> CompletableFuture<U> newIncompleteFuture() {
    //   return new WorkerExecutorFuture(WorkerExecutorHelper workerExecutor);
    // }
}

package de.undercouch.gradle.tasks.download.internal;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Executes jobs asynchronously with an {@link ExecutorService} on Gradle
 * versions where the Worker API is not available
 * @author Michel Kraemer
 */
public class LegacyWorkerExecutorHelper extends WorkerExecutorHelper {
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final Queue<Future<Void>> futures = new ConcurrentLinkedQueue<>();

    @Override
    public void submit(Job job) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        futures.add(f);
        executorService.submit(() -> {
            try {
                job.run();
                f.complete(null);
                futures.remove(f);
            } catch (IOException e) {
                f.completeExceptionally(e);
            }
        });
    }

    @Override
    public void await() {
        Future<Void> f;
        while ((f = futures.poll()) != null) {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean needsAwait() {
        return true;
    }
}

package it.kristikomini.modern.acl;

import it.kristikomini.modern.gateway.LegacyUnavailableException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs independent external lookups <b>concurrently</b> on Java 21 virtual threads.
 *
 * <p>The legacy code performed several independent enrichment calls (policy → holder registry →
 * claims history → risk score) one after another, so latency was the <i>sum</i>. Because these
 * calls do not depend on each other, they can run at once; with virtual threads the fan-out is
 * cheap (no pool sizing, one lightweight thread per task) and the latency becomes roughly the
 * <i>slowest</i> call instead of the sum. See {@code docs/PERFORMANCE.md}.
 */
@Component
public class VirtualThreadFanout {

    /**
     * Submits every task to a new virtual-thread-per-task executor, waits for all, and returns the
     * results in input order. If any task fails, the whole fan-out fails with a clean
     * {@link LegacyUnavailableException} (an all-or-nothing enrichment).
     */
    public <T> List<T> runAll(List<Callable<T>> tasks) {
        // try-with-resources: close() on this executor blocks until all tasks finish (Java 21).
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(task));
            }
            List<T> results = new ArrayList<>(tasks.size());
            for (Future<T> future : futures) {
                results.add(join(future));
            }
            return results;
        }
    }

    private <T> T join(Future<T> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new LegacyUnavailableException("Parallel legacy lookup failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LegacyUnavailableException("Parallel legacy lookup interrupted", e);
        }
    }
}

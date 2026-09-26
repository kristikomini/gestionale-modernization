package it.kristikomini.modern.acl;

import it.kristikomini.modern.gateway.LegacyUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VirtualThreadFanoutTest {

    private final VirtualThreadFanout fanout = new VirtualThreadFanout();

    @Test
    void returnsResultsInInputOrder() {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2, () -> 3);
        assertThat(fanout.runAll(tasks)).containsExactly(1, 2, 3);
    }

    /**
     * Deterministic proof of concurrency: each task must pass through a {@link CyclicBarrier} of N
     * parties. The barrier only trips if all N tasks are running <b>at the same time</b> — if the
     * fan-out ran them sequentially, task 1 would block forever waiting for the others and the test
     * would time out. No sleeps, no flakiness.
     */
    @Test
    void runsTasksConcurrently() {
        int n = 5;
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Callable<Integer>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int id = i;
            tasks.add(() -> {
                barrier.await(); // waits until all N tasks have arrived — requires true concurrency
                return id;
            });
        }

        List<Integer> results = fanout.runAll(tasks);

        assertThat(results).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    void wrapsTaskFailureAsCleanError() {
        List<Callable<Integer>> tasks = List.of(
                () -> 1,
                () -> { throw new RuntimeException("registry down"); });

        assertThatThrownBy(() -> fanout.runAll(tasks))
                .isInstanceOf(LegacyUnavailableException.class);
    }
}

package it.kristikomini.modern;

import it.kristikomini.modern.acl.VirtualThreadFanout;
import it.kristikomini.modern.policy.CoverageEntity;
import it.kristikomini.modern.policy.PolicyEntity;
import it.kristikomini.modern.policy.PolicyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Measures the two headline performance fixes against real PostgreSQL, and guards them against
 * regression. It both documents the numbers (printed to the console, reproduced in
 * {@code docs/PERFORMANCE.md}) and asserts the shape holds: the naive path is N+1, the fixed path
 * is a single query, and the virtual-thread fan-out is far faster than the sequential sum.
 *
 * <p>Skips cleanly without Docker; runs in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PerformanceBenchmarkTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final int POLICIES = 200;
    private static final int COVERAGES_EACH = 3;

    @Autowired PolicyRepository repository;
    @Autowired VirtualThreadFanout fanout;
    @Autowired EntityManagerFactory emf;
    @PersistenceContext EntityManager em;

    @Test
    @Transactional
    void nPlusOneVersusJoinFetch() {
        repository.deleteAll();
        List<PolicyEntity> batch = new ArrayList<>();
        for (int i = 0; i < POLICIES; i++) {
            PolicyEntity p = new PolicyEntity("POL-" + i, "FC" + i, LocalDate.of(2026, 1, 1), new BigDecimal("100.00"));
            for (int c = 0; c < COVERAGES_EACH; c++) {
                p.addCoverage(new CoverageEntity("C" + c, "cov " + c, new BigDecimal("10.00")));
            }
            batch.add(p);
        }
        repository.saveAll(batch);
        em.flush();
        em.clear(); // detach, so the reads below actually hit the DB

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

        long t0 = System.nanoTime();
        long naiveQueries = countQueries(stats, () -> touchCoverages(repository.findAll()));
        long naiveMs = millis(t0);

        em.clear();
        long t1 = System.nanoTime();
        long fetchQueries = countQueries(stats, () -> touchCoverages(repository.findAllWithCoverages()));
        long fetchMs = millis(t1);

        System.out.printf(">>> N+1: %d policies -> naive %d queries (%d ms) | JOIN FETCH %d query (%d ms)%n",
                POLICIES, naiveQueries, naiveMs, fetchQueries, fetchMs);

        assertThat(naiveQueries).isEqualTo(POLICIES + 1L); // 1 for the list + N for lazy coverages
        assertThat(fetchQueries).isEqualTo(1L);            // the whole point of the fix
    }

    @Test
    void virtualThreadFanoutBeatsSequential() {
        int tasks = 100, latencyMs = 20;
        List<Callable<Integer>> work = new ArrayList<>();
        for (int i = 0; i < tasks; i++) {
            final int v = i;
            work.add(() -> { Thread.sleep(latencyMs); return v; });
        }

        long t0 = System.nanoTime();
        List<Integer> results = fanout.runAll(work);
        long fanoutMs = millis(t0);
        long sequentialMs = (long) tasks * latencyMs;

        System.out.printf(">>> fan-out: %d tasks x %d ms -> sequential %d ms, virtual threads %d ms%n",
                tasks, latencyMs, sequentialMs, fanoutMs);

        assertThat(results).hasSize(tasks);
        // Independent blocking calls concurrently: wall-clock should be a small fraction of the sum.
        assertThat(fanoutMs).isLessThan(sequentialMs / 4);
    }

    private static int touchCoverages(List<PolicyEntity> policies) {
        int n = 0;
        for (PolicyEntity p : policies) {
            n += p.getCoverages().size();
        }
        return n;
    }

    private long countQueries(Statistics stats, java.util.function.IntSupplier action) {
        long before = stats.getPrepareStatementCount();
        action.getAsInt();
        return stats.getPrepareStatementCount() - before;
    }

    private static long millis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}

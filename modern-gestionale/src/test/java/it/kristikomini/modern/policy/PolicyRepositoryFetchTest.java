package it.kristikomini.modern.policy;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The payoff of the modernization, proven against real PostgreSQL: {@code findAllWithCoverages()}
 * loads N policies <b>and</b> their coverages in a SINGLE query — versus the legacy {@code 1 + N}
 * pinned by the legacy module's characterisation test.
 *
 * <p>Skips cleanly when Docker is not reachable (local sandbox); runs for real in CI.
 */
@Testcontainers(disabledWithoutDocker = true) // skip cleanly when Docker is absent; run in CI
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PolicyRepositoryFetchTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PolicyRepository repository;

    @PersistenceUnit
    EntityManagerFactory emf;

    @PersistenceContext
    EntityManager em;

    @Test
    void loadsPoliciesAndCoveragesInOneQuery() {
        for (int i = 1; i <= 5; i++) {
            PolicyEntity p = new PolicyEntity("POL-2024-" + String.format("%04d", i),
                    "CF" + i, LocalDate.now(), new BigDecimal("450.00"));
            p.addCoverage(new CoverageEntity("RCA", "Copertura", new BigDecimal("6000000.00")));
            p.addCoverage(new CoverageEntity("FUR", "Furto", new BigDecimal("20000.00")));
            repository.save(p);
        }
        em.flush();
        em.clear(); // detach everything so the read really hits the DB

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        List<PolicyEntity> policies = repository.findAllWithCoverages();
        // Touch coverages — with the fetch join these are already loaded, so no extra queries.
        int totalCoverages = policies.stream().mapToInt(p -> p.getCoverages().size()).sum();

        assertThat(policies).hasSize(5);
        assertThat(totalCoverages).isEqualTo(10);
        assertThat(stats.getPrepareStatementCount())
                .as("N+1 fixed: one query loads policies and coverages")
                .isEqualTo(1);
    }
}

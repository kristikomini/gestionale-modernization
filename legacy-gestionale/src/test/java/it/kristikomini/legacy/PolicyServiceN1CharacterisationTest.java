package it.kristikomini.legacy;

import it.kristikomini.legacy.policy.Coverage;
import it.kristikomini.legacy.policy.Policy;
import it.kristikomini.legacy.soap.PolicyDto;
import it.kristikomini.legacy.soap.PolicyServiceImpl;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Characterisation test that <b>pins the N+1 problem</b> against a real PostgreSQL (Testcontainers).
 *
 * <p>It asserts that {@code listPolicies()} issues exactly {@code 1 + N} SQL statements for
 * {@code N} policies — one for the list, one more per policy to lazily load its coverages. This is
 * the current behaviour, recorded so the modern module's {@code JOIN FETCH} fix can be proven to
 * reduce it to a single query without changing the SOAP output.
 *
 * <p>Skips cleanly when Docker is not available, so a laptop without Docker still gets a green
 * {@code mvn test}; CI (which has Docker) runs it for real.
 */
public class PolicyServiceN1CharacterisationTest {

    private static final int POLICY_COUNT = 5;
    private static final int COVERAGES_PER_POLICY = 2;

    private static PostgreSQLContainer<?> postgres;
    private static EntityManagerFactory emf;

    @BeforeClass
    public static void startDb() {
        Assume.assumeTrue("Docker not available — skipping N+1 characterisation",
                DockerClientFactory.instance().isDockerAvailable());

        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("gestionale").withUsername("app").withPassword("app");
        postgres.start();

        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("javax.persistence.jdbc.url", postgres.getJdbcUrl());
        overrides.put("javax.persistence.jdbc.user", postgres.getUsername());
        overrides.put("javax.persistence.jdbc.password", postgres.getPassword());
        overrides.put("hibernate.hbm2ddl.auto", "create");
        emf = Persistence.createEntityManagerFactory("legacy-gestionale", overrides);

        seed();
    }

    @AfterClass
    public static void stopDb() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    private static void seed() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 1; i <= POLICY_COUNT; i++) {
            Policy p = new Policy("POL-2024-" + String.format("%04d", i),
                    "CF" + i, new Date(), new BigDecimal("450.00"));
            for (int c = 0; c < COVERAGES_PER_POLICY; c++) {
                p.addCoverage(new Coverage("RCA" + c, "Copertura " + c, new BigDecimal("6000000.00")));
            }
            em.persist(p);
        }
        em.getTransaction().commit();
        em.close();
    }

    @Test
    public void listPoliciesIssuesOnePlusNQueries() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        long before = stats.getPrepareStatementCount();

        PolicyServiceImpl service = new PolicyServiceImpl(emf);
        List<PolicyDto> dtos = service.listPolicies();

        long statements = stats.getPrepareStatementCount() - before;

        assertEquals("all policies returned", POLICY_COUNT, dtos.size());
        // The pinned quirk: 1 query for the policies + 1 per policy for its coverages.
        assertEquals("N+1: expected 1 + " + POLICY_COUNT + " statements",
                1 + POLICY_COUNT, statements);
        assertTrue("coverages were loaded", dtos.get(0).getCoperture().size() == COVERAGES_PER_POLICY);
    }
}

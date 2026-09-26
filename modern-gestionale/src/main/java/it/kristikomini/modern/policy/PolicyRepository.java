package it.kristikomini.modern.policy;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository — the modern replacement for the hand-rolled legacy DAO.
 *
 * <p>The whole point of the migration's performance story lives in {@link #findAllWithCoverages()}:
 * a single {@code JOIN FETCH} loads policies and their coverages in <b>one</b> query, fixing the
 * legacy N+1 (which issued {@code 1 + N}). The characterisation test asserts the SOAP/JSON output
 * is unchanged; this asserts the query count dropped to 1.
 */
public interface PolicyRepository extends JpaRepository<PolicyEntity, Long> {

    /** One query, coverages fetched eagerly via join — the N+1 fix. */
    @Query("SELECT DISTINCT p FROM PolicyEntity p LEFT JOIN FETCH p.coverages ORDER BY p.id")
    List<PolicyEntity> findAllWithCoverages();

    /**
     * Filter by holder fiscal code. The column is indexed by the Flyway migration
     * {@code V1__init_schema.sql}, so this is an index scan, not the legacy sequential scan.
     */
    @EntityGraph(attributePaths = "coverages")
    List<PolicyEntity> findByHolderFiscalCode(String holderFiscalCode);

    @EntityGraph(attributePaths = "coverages")
    Optional<PolicyEntity> findByPolicyNumber(String policyNumber);
}

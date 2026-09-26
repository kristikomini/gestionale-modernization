package it.kristikomini.legacy.policy;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * A hand-rolled DAO over the {@link EntityManager} — the shape of persistence code in a lot of
 * pre-Spring-Data legacy systems.
 *
 * <p>The methods here reproduce two classic performance anti-patterns on purpose. They are
 * documented, then fixed on the modern side, and the difference is what the
 * {@code docs/PERFORMANCE.md} benchmark measures.
 */
public class PolicyRepository {

    private final EntityManager em;

    public PolicyRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Lists all policies. The query fetches only the policies; because {@code coverages} is
     * {@code LAZY}, any caller that then reads {@code policy.getCoverages()} triggers a fresh
     * SELECT per policy — the <b>N+1 problem</b>. With P policies this is 1 + P queries.
     *
     * <p>See {@code PolicyServiceImpl}, which does exactly that when it maps to SOAP DTOs.
     */
    public List<Policy> findAll() {
        TypedQuery<Policy> q = em.createQuery("SELECT p FROM Policy p ORDER BY p.id", Policy.class);
        return q.getResultList();
    }

    /**
     * Filters by holder fiscal code. The column is not indexed (see {@link Policy}), so on a
     * large table this is a sequential scan. The modern module adds the index and shows the
     * plan change.
     */
    public List<Policy> findByHolderFiscalCode(String fiscalCode) {
        TypedQuery<Policy> q = em.createQuery(
                "SELECT p FROM Policy p WHERE p.holderFiscalCode = :fc ORDER BY p.id", Policy.class);
        q.setParameter("fc", fiscalCode);
        return q.getResultList();
    }

    public Policy findByPolicyNumber(String policyNumber) {
        TypedQuery<Policy> q = em.createQuery(
                "SELECT p FROM Policy p WHERE p.policyNumber = :n", Policy.class);
        q.setParameter("n", policyNumber);
        List<Policy> results = q.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public void save(Policy policy) {
        em.persist(policy);
    }
}

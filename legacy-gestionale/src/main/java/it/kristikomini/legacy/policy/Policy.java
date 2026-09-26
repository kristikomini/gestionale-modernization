package it.kristikomini.legacy.policy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An insurance policy — the aggregate root of the legacy domain.
 *
 * <p>This class is deliberately written in the style you inherit, not the style you would
 * choose today. Two things here are on purpose and are the subject of the modernization:
 *
 * <ul>
 *   <li><b>{@code java.util.Date}</b> for {@link #startDate}: mutable and not thread-safe,
 *       formatted with a shared {@code SimpleDateFormat} in the SOAP layer. The modern side
 *       migrates this to {@code java.time.LocalDate}.</li>
 *   <li><b>{@code holderFiscalCode} has no index</b> even though it is the field the service
 *       filters on. On a real dataset this is a sequential scan; the modern side adds the
 *       index via a Flyway migration and shows the {@code EXPLAIN} before/after.</li>
 * </ul>
 *
 * <p>Uses the {@code javax.persistence} namespace (Jakarta EE 8) — the OpenRewrite recipe
 * rewrites these imports to {@code jakarta.persistence} in the modern module.
 */
@Entity
@Table(name = "policy")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    /** The field the service filters by — intentionally NOT indexed here. */
    @Column(name = "holder_fiscal_code", nullable = false)
    private String holderFiscalCode;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "annual_premium", nullable = false)
    private BigDecimal annualPremium;

    /**
     * Coverages are {@code LAZY} (the JPA default for collections). Combined with the
     * service listing policies and then reading each policy's coverages, this produces the
     * classic <b>N+1 select</b> problem: one query for the policies, then one more per policy
     * for its coverages. The modern repository fixes it with a {@code JOIN FETCH}.
     */
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Coverage> coverages = new ArrayList<Coverage>();

    protected Policy() {
        // JPA requires a no-arg constructor.
    }

    public Policy(String policyNumber, String holderFiscalCode, Date startDate, BigDecimal annualPremium) {
        this.policyNumber = policyNumber;
        this.holderFiscalCode = holderFiscalCode;
        this.startDate = startDate;
        this.annualPremium = annualPremium;
    }

    public void addCoverage(Coverage coverage) {
        coverage.setPolicy(this);
        this.coverages.add(coverage);
    }

    public Long getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getHolderFiscalCode() {
        return holderFiscalCode;
    }

    public Date getStartDate() {
        return startDate;
    }

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }

    public List<Coverage> getCoverages() {
        return coverages;
    }
}

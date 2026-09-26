package it.kristikomini.modern.policy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern policy entity. Same domain as the legacy one, but on the modern stack — this is the
 * "after" of the migration:
 *
 * <ul>
 *   <li>{@code jakarta.persistence} instead of {@code javax.persistence}.</li>
 *   <li>{@code java.time.LocalDate} instead of {@code java.util.Date} — immutable, thread-safe,
 *       so no shared-{@code SimpleDateFormat} race.</li>
 *   <li>The schema (including the index on {@code holder_fiscal_code}) is owned by Flyway, not
 *       by {@code ddl-auto}; {@code ddl-auto=validate} only checks the entity matches it.</li>
 * </ul>
 *
 * The N+1 is not fixed on the entity (coverages stay {@code LAZY}, which is the right default);
 * it is fixed at the query in {@link PolicyRepository} with a fetch join.
 */
@Entity
@Table(name = "policy")
public class PolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(name = "holder_fiscal_code", nullable = false)
    private String holderFiscalCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "annual_premium", nullable = false)
    private BigDecimal annualPremium;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CoverageEntity> coverages = new ArrayList<>();

    protected PolicyEntity() {
    }

    public PolicyEntity(String policyNumber, String holderFiscalCode, LocalDate startDate, BigDecimal annualPremium) {
        this.policyNumber = policyNumber;
        this.holderFiscalCode = holderFiscalCode;
        this.startDate = startDate;
        this.annualPremium = annualPremium;
    }

    public void addCoverage(CoverageEntity coverage) {
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }

    public List<CoverageEntity> getCoverages() {
        return coverages;
    }
}

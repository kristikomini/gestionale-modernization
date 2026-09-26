package it.kristikomini.legacy.policy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * A single coverage line on a {@link Policy} (e.g. "third-party liability", "theft").
 * The many-to-one back to the policy is {@code LAZY} — reading {@code coverage.getPolicy()}
 * on a detached instance is one of the ways legacy code trips over {@code LazyInitializationException}.
 */
@Entity
@Table(name = "coverage")
public class Coverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "insured_amount", nullable = false)
    private BigDecimal insuredAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    protected Coverage() {
    }

    public Coverage(String code, String description, BigDecimal insuredAmount) {
        this.code = code;
        this.description = description;
        this.insuredAmount = insuredAmount;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getInsuredAmount() {
        return insuredAmount;
    }

    public Policy getPolicy() {
        return policy;
    }

    void setPolicy(Policy policy) {
        this.policy = policy;
    }
}

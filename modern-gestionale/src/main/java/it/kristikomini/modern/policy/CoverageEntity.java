package it.kristikomini.modern.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "coverage")
public class CoverageEntity {

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
    private PolicyEntity policy;

    protected CoverageEntity() {
    }

    public CoverageEntity(String code, String description, BigDecimal insuredAmount) {
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

    public PolicyEntity getPolicy() {
        return policy;
    }

    void setPolicy(PolicyEntity policy) {
        this.policy = policy;
    }
}

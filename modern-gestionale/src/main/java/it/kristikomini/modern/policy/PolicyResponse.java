package it.kristikomini.modern.policy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The modern REST representation of a policy — a Java {@code record}, never the {@code @Entity}
 * (shared standards, rule 3). Clean English names, a real {@code LocalDate}, an immutable shape.
 */
public record PolicyResponse(
        String policyNumber,
        String holderFiscalCode,
        LocalDate startDate,
        BigDecimal annualPremium,
        List<CoverageResponse> coverages) {
}

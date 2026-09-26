package it.kristikomini.modern.policy;

import java.math.BigDecimal;

public record CoverageResponse(String code, String description, BigDecimal insuredAmount) {
}

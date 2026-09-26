package it.kristikomini.modern.gateway;

import it.kristikomini.modern.policy.PolicyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global error handling as RFC 7807 {@link ProblemDetail} (shared standards, rule 4). Never a raw
 * stack trace, never a bare 500 — a stable, machine-readable {@code type}/{@code title}/{@code detail}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PolicyNotFoundException.class)
    public ProblemDetail handleNotFound(PolicyNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Policy not found");
        pd.setType(URI.create("https://kristikomini.it/problems/policy-not-found"));
        return pd;
    }

    /**
     * When the Anti-Corruption Layer cannot reach the legacy backend (circuit open / timeout),
     * the caller gets a clean 503 with a Retry hint — not a hang and not a 500.
     */
    @ExceptionHandler(LegacyUnavailableException.class)
    public ProblemDetail handleLegacyDown(LegacyUnavailableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        pd.setTitle("Legacy system temporarily unavailable");
        pd.setType(URI.create("https://kristikomini.it/problems/legacy-unavailable"));
        return pd;
    }
}

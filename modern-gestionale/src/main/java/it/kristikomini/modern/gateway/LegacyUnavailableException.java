package it.kristikomini.modern.gateway;

/**
 * Raised by the Anti-Corruption Layer's fallback when the legacy SOAP backend is unreachable
 * (Resilience4j circuit open, retries exhausted, or time limit exceeded). Translated to a clean
 * 503 by {@link ApiExceptionHandler} instead of leaking the underlying SOAP/HTTP failure.
 */
public class LegacyUnavailableException extends RuntimeException {

    public LegacyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

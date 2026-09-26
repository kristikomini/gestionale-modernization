# ADR 0002 — Anti-Corruption Layer around the legacy SOAP service

- **Status:** accepted
- **Date:** (build date)

## Context
The legacy SOAP/JAX-WS service exposes a data model shaped by fifteen years of accretion:
XML types with cryptic names, `xsd:dateTime` strings, nullable everything, and error semantics
encoded in string fields. Letting that model leak into the new code would corrupt the new domain.

## Decision
Introduce an **Anti-Corruption Layer (ACL)**: a boundary component in `modern` that
- consumes the legacy WSDL via a generated JAX-WS client,
- wraps every call in **Resilience4j** (circuit breaker, retry with backoff, time limiter) so a
  slow or failing legacy backend degrades gracefully instead of cascading,
- **translates** the legacy XML model into the clean modern domain (records), isolating all the
  ugliness in one place.

## Consequences
- The modern domain stays clean; the legacy vocabulary exists only inside the ACL mapper.
- One well-tested translation layer instead of legacy concepts scattered everywhere.
- Resilience policies (timeouts, breaker thresholds) are explicit config, not hidden defaults.
- When a route is fully migrated (data owned by modern), its ACL path is deleted.

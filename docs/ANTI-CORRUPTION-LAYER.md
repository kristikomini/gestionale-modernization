# Anti-Corruption Layer (ACL)

```
 modern domain (records)          ACL boundary                 legacy SOAP
 ┌───────────────────┐    ┌──────────────────────────┐    ┌────────────────┐
 │ Policy (clean)     │◀──│ LegacyPolicyMapper         │◀──│ WSDL / JAX-WS  │
 │ java.time, Optional│    │ Resilience4j-wrapped client│    │ javax types    │
 └───────────────────┘    └──────────────────────────┘    └────────────────┘
                              circuit breaker / retry / time limiter
```

## What lives here
- **Generated client** from the legacy WSDL (`wsimport` / cxf-codegen), the only place the
  `javax`/SOAP types are referenced.
- **Resilience4j** decorators: `CircuitBreaker` (open after N% failures), `Retry` (bounded, with
  backoff, only on transient faults), `TimeLimiter` (cap the wait on a slow legacy call).
- **Mapper** (hand-written or MapStruct) legacy XML DTO → modern domain record. Cryptic fields,
  string dates and null-encoded errors are normalised here and nowhere else.

## Why wrap resilience *here* and not in the controller
The legacy system is the unreliable dependency. Putting the breaker/retry at the ACL boundary
means every consumer of the legacy service gets the same protection, and the policy is defined
once, next to the thing it protects.

## Test
Stub the SOAP endpoint (WireMock) to fail / hang; assert the breaker opens, the fallback fires,
and the modern API returns a clean RFC 7807 error rather than hanging or 500-ing.

# Gestionale modernization — strangler fig, SOAP→REST, `javax`→`jakarta`

Taking a legacy **Java 8** application — the kind that talks **SOAP (JAX-WS)**, runs on
`javax.*`, and hides Hibernate performance bugs — to **Spring Boot 3 / Java 21** the way it is
actually done in industry: **one endpoint at a time, behind a gateway, with the old system
still running and protected by an Anti-Corruption Layer.** No big-bang rewrite.

This is the single most under-represented skill on junior CVs and the one Italian consultancies
and product companies need most (Reply, NTT Data, Fincons, BIP; banking & insurance in Milano,
Bologna, Verona): **you are hired to change code that already exists.**

> Follows the shared [engineering standards](../ENGINEERING-STANDARDS.md). Migration decisions
> are logged as ADRs in [`docs/adr/`](docs/adr/).

## The two modules

| Module | Role | Stack |
|--------|------|-------|
| `legacy-gestionale` | The system "you inherited". Deliberately old: Java 8, a **SOAP/JAX-WS** service (policy/invoice management) over WSDL, `javax.*`, `java.util.Date`, an **N+1** Hibernate bug, a missing index, synchronous blocking calls. | Java 8, `javax.*`, JAX-WS, Hibernate 5 |
| `modern-gestionale` | The strangler. Spring Boot 3 / Java 21. A **gateway** routes each migrated route here; unmigrated routes proxy to legacy. Contains the **Anti-Corruption Layer** that consumes the legacy SOAP and exposes clean **REST + OpenAPI 3**. | Java 21, Spring Boot 3, `jakarta.*`, Resilience4j, Flyway |

## The migration, as a sequence of defensible moves

1. **Characterisation tests first** — pin what legacy *currently* does (bugs included) so a
   change is distinguishable from a regression. → [`docs/CHARACTERISATION.md`](docs/CHARACTERISATION.md)
2. **`javax`→`jakarta` with OpenRewrite**, then hand-fix what the recipe can't see — names inside
   strings (`persistence.xml`, `Class.forName`, XML/log config). → [`docs/JAVAX-TO-JAKARTA.md`](docs/JAVAX-TO-JAKARTA.md)
3. **Anti-Corruption Layer** — a client that consumes the legacy SOAP XML, wrapped in
   **Resilience4j** (circuit breaker, retry, time limiter) so a flaky legacy backend can't take
   the new system down, and translates the legacy model into a clean domain. → [`docs/ANTI-CORRUPTION-LAYER.md`](docs/ANTI-CORRUPTION-LAYER.md)
4. **Modernize the hot paths** — fix the N+1 (`JOIN FETCH`/entity graph), add the index, migrate
   `java.util.Date` → `java.time`, and replace blocking sequential external lookups with **Java 21
   virtual threads** / `CompletableFuture` pipelines. → [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md)
5. **Strangle route by route** — flip one gateway route to modern, keep the characterisation test
   green, soak, then delete the legacy handler. → [`docs/STRANGLER.md`](docs/STRANGLER.md)

## What this demonstrates (CV bullets)

*Proven by tests in this repo today:*
- Modernized a Java 8 / `javax` / SOAP gestionale to Spring Boot 3 / Java 21 via the strangler-fig
  pattern with an Anti-Corruption Layer — the migrate/not-migrated route decision is one config
  flip, unit-tested both ways so a migrated route never touches the legacy system.
- Wrapped the legacy SOAP integration in Resilience4j: retry-wraps-circuit-breaker; when the
  backend fails the breaker opens and the API fails fast with a clean RFC 7807 503 instead of
  hanging — proven by a circuit-breaker unit test.
- Fixed a Hibernate **N+1 from `201` to `1` query** (200 policies) with `JOIN FETCH`, measured by
  `PerformanceBenchmarkTest` against real Postgres (Testcontainers) — ~6.8× faster wall-clock here,
  and far more on a remote DB where each round-trip counts.
- Migrated `java.util.Date` → `java.time` (killing a shared-`SimpleDateFormat` race) and ran
  independent legacy lookups concurrently on Java 21 virtual threads: **100 × 20 ms tasks went from
  2000 ms sequential to 44 ms** (~45×), measured in the same benchmark.

*Measured — see [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md):* N+1 201→1 query; virtual-thread
fan-out ~45× on 100 independent calls.

## Run it

```bash
docker compose up          # legacy (SOAP) + modern (REST gateway + ACL) + postgres
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the routing model and diagrams.

## Course topics exercised

Legacy I/O vs NIO.2, `Date`/`Calendar` → `java.time`, XML marshalling (JAXB/JAX-WS), classloaders,
generics in the ACL mapper. Academy modules: 28 (the code you inherit), 12 (expected failure /
testing legacy), 22 (JPA & N+1), 25 (SQL & migrations), 08 (concurrency / virtual threads).

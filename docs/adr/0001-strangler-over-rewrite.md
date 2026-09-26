# ADR 0001 — Strangler fig over big-bang rewrite

- **Status:** accepted
- **Date:** (build date)

## Context
We inherited a Java 8 / `javax` SOAP application with undocumented behaviour and live users.
The business wants it on a modern, maintainable stack. A full rewrite would have to reproduce
every quirk of a system that is still changing, and cannot ship until it is 100% done.

## Decision
Apply the **strangler-fig** pattern: place a gateway in front of the legacy system, route traffic
through it, and migrate one endpoint at a time to a new Spring Boot 3 / Java 21 service. The
legacy system keeps serving everything not yet migrated and shrinks over time.

## Consequences
- Every step is small, independently shippable, and reversible (flip the route back).
- Requires a gateway and a period where two systems run side by side against one database.
- Needs characterisation tests as a safety net before any behaviour is touched (see ADR 0003).
- Migration progress is measurable (routes migrated / total).

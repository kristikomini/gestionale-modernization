# Architecture — strangler-fig modernization

## The idea in one diagram

```
                         ┌───────────────────────────┐
   client  ─────────────▶│         Gateway           │
                         │  (Spring Cloud Gateway)   │
                         │  route table decides:     │
                         │   migrated? → modern       │
                         │   else      → legacy       │
                         └─────────┬─────────┬────────┘
                                   │         │
                    migrated route │         │ not-yet-migrated route
                                   ▼         ▼
                    ┌──────────────────┐   ┌────────────────────┐
                    │ modern-gestionale │   │  legacy-gestionale │
                    │ Spring Boot 3     │   │  Java 8, javax.*   │
                    │ Java 21, jakarta.*│   │  (frozen, shrinking)│
                    └────────┬─────────┘   └─────────┬──────────┘
                             │                       │
                             └──────────┬────────────┘
                                        ▼
                                 ┌──────────────┐
                                 │  PostgreSQL   │  (shared during migration;
                                 └──────────────┘   modern owns the schema via Flyway)
```

## Why a gateway and not a rewrite

A big-bang rewrite has to reproduce every undocumented behaviour of the old system while that
system keeps changing. The **strangler fig** puts the new system *in front*, moves one route
at a time, and lets the old system shrink until it can be deleted. Each step is small,
reversible (flip the route back), and independently shippable.

## Data during migration

The two apps share one Postgres instance. To avoid two ORMs fighting over the schema, **the
modern module owns migrations** (Flyway); the legacy module is pinned to `validate`-only and
never mutates DDL. When a table's last legacy reader is strangled, its access moves fully to
modern.

## Routing model

The strangler decision — for each resource, is it served from the modern DB or from the legacy
system via the ACL — is a **config flip**, not a code change. Two equivalent ways to realise it:

- **Application-level router (implemented here).** `PolicyQueryService` reads
  `strangler.policies.migrated`; `true` serves from the modern repository, `false` serves through
  the ACL (`LegacyPolicyClient`). Unit-tested both ways
  (`PolicyQueryServiceTest`) — a migrated route never touches legacy, and vice versa. This is the
  testable form when the modern app fronts both paths.
- **Edge gateway (deployment alternative).** When legacy and modern are separate processes, an
  edge gateway (Spring Cloud Gateway) holds the same `METHOD + path → legacy|modern` table and
  routes at the network edge; migrating a route is a one-line gateway config flip. Same decision,
  moved to the edge.

Either way the *decision* to cut over stays separate from the *work* of building the replacement.

## Test strategy

- **Characterisation tests** run against the legacy module and capture current outputs
  verbatim (see [`CHARACTERISATION.md`](CHARACTERISATION.md)). They are the safety net.
- **Contract**: the modern replacement for a route must satisfy the same characterisation test
  before its route is flipped.
- **Integration**: Testcontainers Postgres for both modules; a gateway smoke test asserts the
  route table sends each path to the right backend.

## Sequence of a single route migration

1. Characterisation test exists and is green against legacy.
2. Build the modern endpoint; run the *same* characterisation test against it.
3. Flip the gateway route `modern`.
4. Observe (metrics/logs) in production; keep legacy code in place.
5. After a soak period, delete the legacy handler.

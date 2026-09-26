# Performance — the legacy bugs, and the modern fixes

The legacy module ships real, classic performance problems. Fixing them (and measuring the
before/after) is the interview story.

## 1. Hibernate N+1
The legacy policy-list loads each policy's coverages lazily → 1 query for the list + N for the
coverages. **Fix:** `JOIN FETCH` / `@EntityGraph` in the modern repository.

Measured by `PerformanceBenchmarkTest` against real PostgreSQL, 200 policies × 3 coverages:

| | Queries | Wall-clock |
|---|---|---|
| Naive (lazy, N+1) | **201** (1 + 200) | 389 ms |
| Modern (`JOIN FETCH`) | **1** | 57 ms |

The query count is the durable number (it does not depend on the machine): 201 → 1. The wall-clock
(~6.8× here) grows with round-trip latency — on a remote DB the N+1 penalty is far worse.

## 2. Missing index
A frequent filter (`policy.holder_fiscal_code`) has no index → sequential scan. **Fix:** Flyway
migration adding the index; show the `EXPLAIN` plan before/after.

## 3. `java.util.Date` concurrency
Legacy shares a `SimpleDateFormat` across threads (not thread-safe) → corrupted parses under load.
**Fix:** migrate to `java.time` (`LocalDate`/`Instant`, immutable, thread-safe).

## 4. Synchronous blocking external lookups
Legacy calls independent external services sequentially, so latency is the **sum**. **Fix:** run
them concurrently on **Java 21 virtual threads** (`Executors.newVirtualThreadPerTaskExecutor()`),
so latency becomes roughly the **slowest** call.

Measured by `PerformanceBenchmarkTest`, 100 independent tasks of 20 ms each:

| | Wall-clock |
|---|---|
| Sequential (sum) | 2000 ms |
| Virtual-thread fan-out | **44 ms** (≈ max + overhead) |

About **45×** here; the point is that wall-clock stays flat as you add independent calls, instead of
growing linearly.

> Every number here comes from `PerformanceBenchmarkTest`, a repeatable integration measurement
> against real PostgreSQL (not an estimate) — see the shared
> [engineering standards](../../ENGINEERING-STANDARDS.md), rule 7. Re-run with
> `mvn test -pl modern-gestionale -Dtest=PerformanceBenchmarkTest` (needs Docker).

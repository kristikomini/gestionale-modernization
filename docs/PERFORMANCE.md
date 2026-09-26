# Performance — the legacy bugs, and the modern fixes

The legacy module ships real, classic performance problems. Fixing them (and measuring the
before/after) is the interview story.

## 1. Hibernate N+1
The legacy policy-list loads each policy's coverages lazily → 1 query for the list + N for the
coverages. **Fix:** `JOIN FETCH` / `@EntityGraph` in the modern repository.

| | Queries | p95 |
|---|---|---|
| Legacy (N+1) | `<1+N>` | `<N>` ms |
| Modern (`JOIN FETCH`) | 1 | `<N>` ms |

## 2. Missing index
A frequent filter (`policy.holder_fiscal_code`) has no index → sequential scan. **Fix:** Flyway
migration adding the index; show the `EXPLAIN` plan before/after.

## 3. `java.util.Date` concurrency
Legacy shares a `SimpleDateFormat` across threads (not thread-safe) → corrupted parses under load.
**Fix:** migrate to `java.time` (`LocalDate`/`Instant`, immutable, thread-safe).

## 4. Synchronous blocking external lookups
Legacy calls three external services sequentially. **Fix:** run them concurrently with **Java 21
virtual threads** (`Executors.newVirtualThreadPerTaskExecutor()`) or a `CompletableFuture`
pipeline. Measure wall-clock: `<sum>` ms → `~max` ms.

> Every number here must come from a JMH microbenchmark or a repeatable integration measurement,
> not an estimate — see the shared [engineering standards](../../ENGINEERING-STANDARDS.md), rule 7.

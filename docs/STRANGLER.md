# Strangler-fig route migration

The gateway owns a route table. Migrating an endpoint is a config flip, not a code change to
either backend — which keeps the *decision* to cut over separate from the *work* of building
the replacement, and makes every step reversible.

## Route table (illustrative)

| Method + path        | Target  | Status        |
|----------------------|---------|---------------|
| `GET /orders`        | modern  | migrated      |
| `GET /orders/{id}`   | modern  | migrated      |
| `POST /orders`       | legacy  | not yet       |
| `* /stock/**`        | legacy  | not yet       |
| `* /invoices/**`     | legacy  | not yet       |

## Migration checklist per route

- [ ] Characterisation test green against legacy.
- [ ] Modern endpoint built; same test green against modern.
- [ ] Flip route → modern in gateway config; redeploy gateway only.
- [ ] Watch metrics/logs (error rate, latency) through the soak window.
- [ ] Delete the legacy handler once the route has been on modern with no regressions.

## Why route-level, not feature-flag-in-code

Routing at the edge means the two backends never need to know about each other, and a bad
cutover is undone by flipping one line back — no redeploy of the thing that broke.

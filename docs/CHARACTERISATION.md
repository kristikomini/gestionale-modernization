# Characterisation tests

> A characterisation test records what the code **currently does** — not what it *should*
> do. Its job is to make a behaviour change visible, so you can tell a refactor from a
> regression. Bugs are pinned too, on purpose: if the legacy system rounds the wrong way,
> the test asserts the wrong-way result, and the migration is expected to reproduce it
> until someone decides to fix it deliberately.

## Plan (fill during build)

1. Enumerate the legacy endpoints (orders, stock, invoice) and their observable outputs.
2. For each, seed a known DB state and snapshot the response (status, body, headers) and any
   DB side effects. Store snapshots as fixtures.
3. Run the identical suite against the modern replacement before flipping its gateway route;
   the route is not migrated until the suite is green against modern.

## Known legacy quirks to pin (to be discovered/seeded)

- [ ] Invoice total rounding behaviour (half-up vs half-even).
- [ ] Stock decrement ordering under concurrent orders (the bug we will *not* silently fix).
- [ ] Date/timezone handling on the order-created field.

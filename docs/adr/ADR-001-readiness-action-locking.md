# ADR-001: Locking strategy for readiness action updates

- Status: Accepted for the current prototype.
- Context: API and web updates can target the same `readiness_action` row. A detached read-modify-write can otherwise overwrite another writer's newer row state.
- Decision: Use PostgreSQL optimistic locking through JPA `@Version` for normal updates. Retry an optimistic conflict up to five attempts with a small randomized back-off. Keep an explicit `PESSIMISTIC_WRITE` path in the integration benchmark for comparison.
- Rationale: Readiness updates are expected to have low contention, so optimistic locking avoids holding a database lock while applying ordinary patches. If contention is sustained, optimistic retries increase latency and can exhaust the bound; pessimistic locking serializes writers and may increase waiting or deadlock exposure when transactions lock rows in different orders.
- Evidence: `WorkbenchIntegrationTest.readinessActionConcurrentUpdatesAreNotLostAndCompareLockModes` runs 50 concurrent writers for five rounds per strategy against Testcontainers PostgreSQL. Record measured timings and retry/conflict totals in `FINDINGS.md` after the test runs in an environment with Docker available.
- Scope: Synthetic prototype data and workload only; this is not evidence of bank-scale behavior.

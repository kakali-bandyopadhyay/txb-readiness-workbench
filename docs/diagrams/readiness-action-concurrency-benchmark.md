# Readiness action concurrency benchmark

```mermaid
flowchart LR
    Gate[50 worker threads and CountDownLatch start gate] -->|same action id, increment update_count| Choice{Lock mode}
    Choice -->|@Version and bounded retry| Optimistic[Optimistic update]
    Choice -->|PESSIMISTIC_WRITE| Pessimistic[SELECT FOR UPDATE update]
    Optimistic --> PG[(Testcontainers PostgreSQL 17.6)]
    Pessimistic --> PG
    PG --> Result[5 runs per mode; median wall time, conflicts, retries, final update_count]
```

The test measures the two persistence strategies against the same row and verifies every increment is retained.

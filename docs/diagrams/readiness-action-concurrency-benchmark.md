# Readiness action concurrency benchmark

```mermaid
flowchart LR
    Warmup[Unmeasured 50-writer warm-up] --> Gate[50 worker threads and CountDownLatch start gate]
    Gate -->|same action id, increment update_count| Choice{Alternating mode order}
    Choice -->|@Version, bounded retry, gate disabled| Optimistic[Pure optimistic update]
    Choice -->|PESSIMISTIC_WRITE| Pessimistic[SELECT FOR UPDATE update]
    Optimistic --> PG[(Testcontainers PostgreSQL 17.6)]
    Pessimistic --> PG
    PG --> Result[5 measured pairs per invocation; median and range per mode]
    Result --> Summary[3 invocations; median of medians and overall min–max]
```

The test measures pure optimistic retries with the optional in-process gate disabled against pessimistic writes on the same row. It alternates which mode runs first and reports exhaustion counts as well as retained updates. The optional gate is a separate mitigation variant and only coordinates retries within one JVM.

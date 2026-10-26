# Architecture

All examples and records are synthetic. The application is a modular monolith for a designed scenario.

## System context

```mermaid
flowchart LR
  Browser[Browser · demo users] -->|HTML forms / JSON REST| App[Spring Boot modular monolith]
  App -->|JPA / Flyway| DB[(PostgreSQL)]
  App -->|health probe| Health[Actuator]
  App -->|publish / consume events| Events[(Kafka · KRaft)]
  Swagger[OpenAPI / Swagger UI] --> App
```

Phase 2 Kafka topics are available, but the scheduled producer is disabled by default (`demo.events.enabled=false`). When enabled, it publishes only while the release lifecycle status is `LIVE`, reached by a readiness lead's recorded Go or Conditional Go decision. The consumer routes failed and rejected events to `stabilisation-exceptions` and stores queue entries for the Stabilisation page. Stored exception rows are capped at 500 by default, with oldest-first eviction. Topics and queue entries contain synthetic examples only.

## Workflow states

```mermaid
stateDiagram-v2
  [*] --> Draft
  Draft --> InReadiness
  InReadiness --> Decision
  Decision --> InReadiness: revised decision / remaining work
  Decision --> Live: readiness lead records Go
  Decision --> Live: readiness lead records Conditional Go
  Live --> Stabilised
```

These are illustrative workflow states. The prototype seeds the release in readiness. Only a readiness lead's recorded Go or Conditional Go decision moves it to Live; event production also requires the explicit demo property to be enabled.

## Data model

```mermaid
erDiagram
  RELEASE_RECORD ||--o{ IMPACT : maps
  RELEASE_RECORD ||--o{ READINESS_ACTION : tracks
  RELEASE_RECORD ||--o{ UAT_SCENARIO : exercises
  RELEASE_RECORD ||--o{ DEFECT : records
  DEFECT ||--o{ RETEST : retests
  RELEASE_RECORD ||--o{ CONTROL_REVIEW : reviews
  RELEASE_RECORD ||--o{ RELEASE_DECISION : decides
  RELEASE_DECISION ||--o{ DECISION_CONDITION : conditions
  AUDIT_EVENT }o--|| RELEASE_RECORD : references
  RELEASE_RECORD ||--o{ STABILISATION_EXCEPTION : monitors
  RELEASE_RECORD ||--o{ EXECUTIVE_SUMMARY : summarizes
```

Every stored record includes a `synthetic` marker. `AuditEvent` stores actor, role, entity, entity id, action, before/after summary, and timestamp. Relationships are represented by identifiers to keep the demo schema easy to inspect.

## Readiness gate flow

```mermaid
flowchart TD
  A[Read mandatory actions] --> B{Owner completion and evidence?}
  B -->|No| X[Add mandatory-action blocker]
  B -->|Yes| C{Accepted exception with approver and rationale?}
  C -->|Yes| D[Count action complete]
  C -->|No| X
  B -->|No completion evidence| C
  D --> E[Calculate complete / total]
  X --> E
  E --> F{Any open critical defect?}
  F -->|Yes| G[Suggest No-Go]
  F -->|No| H{Other blockers or open high defects?}
  H -->|Yes| I[Suggest Conditional Go]
  H -->|No| J[Suggest Go]
  G --> K[Readiness lead decides and records rationale]
  I --> K
  J --> K
```

An attached document without recorded owner completion never passes the mandatory-action gate. The app's output remains a suggestion; no automatic decision is stored.

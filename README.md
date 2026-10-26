# Transaction Readiness Workbench

A small Java web application that makes readiness for a synthetic transaction banking feature launch visible. It is a **personal prototype showing recent Java work on a current stack; not production banking software and not evidence of bank-scale operations.** All names, dates, figures, evidence references and records are synthetic; decision rules are illustrative.

## Start here

Open `http://localhost:8080`, sign in with a demo account below, then select **Real-time payments for corporate virtual accounts (synthetic)**. The release workspace shows impacted teams, actions, readiness percentage, blockers, and links to UAT, controls, decision, dashboard, and stabilisation pages. Open `/api/docs` for the OpenAPI UI.

The first browser walkthrough starts with one open critical UAT defect. The suggested status is No-Go while that defect is open. Sign in as `lead`, open UAT, record a passing retest for the critical defect, then open Decision. The suggestion changes to Conditional Go while mandatory actions and control reviews remain incomplete. Record Conditional Go with an owned and dated condition. Close the condition from the decision page. The decision is always an explicit action by the readiness lead; calculations only present a suggestion and reasons.

The dashboard region filter uses region-tagged synthetic actions, defects, impacted teams and readiness calculations, so each region displays its own totals.

## Run

Requirements: Java 25 and Docker Desktop with Docker Compose. The Docker CLI must be available in the terminal `PATH`.

```powershell
docker compose up --build
```

Open `http://localhost:8080`. Check `http://localhost:8080/actuator/health` for `{"status":"UP"}`. PostgreSQL runs Flyway migrations and receives synthetic seed records automatically. Kafka runs in single-node KRaft mode. To recreate the sample database from scratch, stop the app and run `docker compose down -v`, then `docker compose up --build`.

Run the test suite with Java 25:

```sh
./mvnw verify
```

Integration tests use Testcontainers and need a running Docker engine. GitHub Actions uses a Maven/Java container to run the same verification.

## Demo accounts

All accounts and passwords are invented, demo-only, and in-memory. These credentials are for local demonstration, not deployment.

| Role | Username | Password | Example capability |
|---|---|---|---|
| Readiness lead | `lead` | `demo-lead` | Record actual release decision and close conditions |
| Operations team owner | `ops` | `demo-ops` | Record action completion with evidence |
| UAT lead | `uat` | `demo-uat` | Record and close defects with retest history |
| Control partner | `control` | `demo-control` | Sign off control reviews |
| Product/engineering partner | `product` | `demo-product` | Update readiness actions |
| Executive viewer | `viewer` | `demo-viewer` | Read-only dashboard access |

## Boundaries and non-goals

- This is an illustrative personal prototype with synthetic records only. No real institution, client, vendor, or employer is represented.
- The readiness calculation counts mandatory actions with recorded completion by an owner plus an evidence reference, or an accepted exception with approver and rationale. Due dates and attachments alone do not complete actions.
- An open critical defect blocks a Go suggestion. Open high defects require named conditions. Control reviews require control-partner sign-off.
- Only a readiness lead can record Go, Conditional Go, or No-Go. Conditional Go requires each condition to have a description, owner, and due date. Conditions remain visible until closed or superseded by a later decision.
- Audit events are append-only from the application: no update or delete endpoint is provided.
- Seeded dates and status values are examples. No claim of production use, measured efficiency, risk reduction, or client impact is made.
- Phase 2 Kafka is a single-node KRaft demo. The scheduled event producer is disabled by default (`demo.events.enabled=false`); set `DEMO_EVENTS_ENABLED=true` to enable it. It publishes only while the release lifecycle status is `LIVE`, which only a readiness lead can set by recording Go or Conditional Go. Failed/rejected events are sent to the stabilisation topic and stored in the visible queue. Stored exception rows are capped at 500 by default; the oldest row is removed when a new row would exceed the cap. Tune this with `DEMO_EVENTS_MAX_RECORDS`. This is illustrative and does not initiate or process payments.
- `SourceGuardTest` scans application source, templates, docs, configuration, and tests for the requested firm and product names plus the prohibited career-related term; its checks use whole-word matching.
- Executive summaries use dashboard data only. The deterministic template is the default; optional model output is labelled as a draft and must be explicitly approved by a readiness lead before the dashboard displays it. Model mode requires `OPENAI_API_KEY` in the environment. No key is stored in this repository.
- No client connectivity, payment initiation, credentials, real operational controls, production identity integration, or production deployment is included.

## Stack

Java 25, Spring Boot 4.1.2, Spring MVC, Validation, Data JPA, Spring Security, Actuator, Thymeleaf, PostgreSQL 17.6, Flyway, Spring Kafka, Apache Kafka 3.9.1 in KRaft mode, springdoc OpenAPI 3.2.0, JUnit 5, Spring Boot tests, and Testcontainers 2.0.3. Docker Compose runs a single application, database, and Kafka broker. The application is a modular monolith.

Java code is grouped by release, readiness, UAT, control, decision, audit, dashboard, stabilisation, and summary features. Each persisted entity has its own file. Controllers translate web/API requests and delegate feature work to services; readiness rules and cross-feature dashboard calculation live in the readiness service.

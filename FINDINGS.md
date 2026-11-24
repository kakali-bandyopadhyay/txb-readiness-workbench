# Findings — TXB Readiness Workbench round 1

## Results

- Region-tagged seeded impacts, actions, and defects are assigned by migration `V3__region_specific_seed.sql`. The dashboard filters actions, defects, impacted teams, completion figures, and readiness blockers to the selected region. North shows 2/6 mandatory actions and 2 teams; Central shows 0/4 and 3 teams.
- `demo.events.enabled` defaults to `false`. `PaymentStatusEventProducer` publishes only if release 1 is `LIVE`; only a readiness-lead decision changes the lifecycle to `LIVE`. `PaymentStatusEventConsumer` validates the synthetic flag and routes failed/rejected events. Stored stabilisation exceptions are capped at 500 by default, with oldest-first eviction. The cap is configurable with `DEMO_EVENTS_MAX_RECORDS`.
- All persisted JPA entities now have separate files in feature packages. API/request and result DTOs are Java records. Controllers delegate to feature services for release, readiness, UAT, controls, decisions, audit, dashboard, stabilisation, and summary work.
- The browser walkthrough completed in the local UI: pass a synthetic critical-defect retest, record a readiness-lead Conditional Go with an owner and due date, then close the condition. The walkthrough reached the dashboard successfully.
- Phase 2 is present and tested. The synthetic Kafka producer remains opt-in and disabled in the default Compose configuration.

## Clean-volume Compose proof

Commands run from `C:\Projects\txb-readiness-workbench`:

```powershell
docker compose down -v
docker compose up --build -d
```

Output summary: the app, PostgreSQL and Kafka containers were stopped and removed; the `readiness-db` volume and Compose network were removed. The image rebuilt from the current source, a new database volume was created, PostgreSQL and Kafka became healthy, and the app started. The fresh seed returned:

- `/actuator/health`: `UP`
- Release list: 1 record, `IN_READINESS`, `synthetic=true`
- Actions: 16; defects: 7; decisions: 0
- North: 2/6 mandatory actions, 2 teams; Central: 0/4, 3 teams

The requested literal Git clone could not be performed: this repository has no remote and the current branch has no commits. No commit was created, per instruction. The clean-volume proof used the current working tree and a newly created Compose database volume. A later UI walkthrough changed the seed; the volume was reset again and the app was left on the fresh seed.

## Runtime and documentation evidence

- `pom.xml`: Java release **25** (`java.version=25`); Spring Boot **4.1.2**.
- Springdoc OpenAPI **3.2.0** is configured. Swagger UI: `http://localhost:8080/api/docs` (verified HTTP 200, redirecting to `/api/swagger-ui/index.html`); generated document: `/v3/api-docs`.
- `docs/ARCHITECTURE.md` contains four Mermaid diagrams: system context, workflow states, data model, and readiness gate flow.
- README boundary sentence, verbatim:

  > personal prototype showing recent Java work on a current stack; not production banking software and not evidence of bank-scale operations.

- `SourceGuardTest.sourceTemplatesDocsAndTestsStayFirmNeutralAndWordFree` scans source, templates, docs, README, build configuration and tests with case-insensitive whole-word checks. It covers the requested employer and product blocklist, including the whole-word-only company token, the requested messaging-product names, and the prohibited career term. Blocked phrases are assembled from fragments so the literal values do not appear as matches in the repository.

## Test coverage map

- Flyway migration on a clean Testcontainers PostgreSQL instance: `WorkbenchIntegrationTest.flywaySeedsOneSyntheticRelease` (the class starts PostgreSQL 17.6 and Kafka Testcontainers, then verifies the synthetic seed).
- Append-only audit API and audit writes: `auditApiHasNoUpdateRoute`, `auditApiHasNoDeleteRoute`, `materialDecisionWritesAuditEvent`.
- Role permissions: readiness lead decision (`onlyReadinessLeadCanRecordDecision`, `readinessLeadCanRecordHumanDecision`, `onlyReadinessLeadCanPostHtmlDecision`); control partner sign-off (`onlyControlPartnerCanSignOff`, `controlPartnerCanSignOff`); executive viewer read-only (`executiveViewerCanReadDashboard`, `executiveViewerCannotCreateAction`, `executiveViewerCannotApproveSummary`).
- Region-specific dashboard values: `dashboardRegionFilterChangesSeededReadinessAndDefects`.
- Event gates and retention: `demoEventProducerIsDisabledByDefault`, `demoEventProducerPublishesOnlyAfterReleaseIsLive`, `stabilisationQueueEvictsOldestAtConfiguredCap`, `syntheticPaymentStatusEventsDefaultToSyntheticLabel`.
- Before switching the benchmark to gate-disabled pure optimism, full `mvn verify` passed with **70 tests, 0 failures, 0 errors, 0 skipped**. The current gate-disabled benchmark correctly fails when it observes exhausted writers.

## Source lines by production package

Nonblank Java source lines, excluding comment-only lines:

| Package | LOC |
|---|---:|
| release | 20 |
| readiness | 138 |
| uat | 38 |
| control | 19 |
| decision | 31 |
| audit | 18 |
| dashboard | 9 |
| stabilisation | 120 |
| summary | 69 |
| root | 171 |

## Test inventory

- `com.synthetic.readiness.ReadinessRulesTest.dueDateDoesNotCompleteAction`
- `com.synthetic.readiness.ReadinessRulesTest.completeWhenOwnerTimestampAndEvidenceExist`
- `com.synthetic.readiness.ReadinessRulesTest.missingActionEvidenceSuggestsConditionalGo`
- `com.synthetic.readiness.ReadinessRulesTest.ordinaryActionCanStillBeReportedCompleteByEvidenceRule`
- `com.synthetic.readiness.ReadinessRulesTest.completionPercentageUsesRoundedNumeratorOverDenominator`
- `com.synthetic.readiness.ReadinessRulesTest.acceptedExceptionCompletesWithApproverAndRationale`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[1]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[2]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[3]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[4]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[5]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[6]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[7]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[8]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[9]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[10]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[11]`
- `com.synthetic.readiness.ReadinessRulesTest.dueDatesNeverCompleteAnUnrecordedAction()[12]`
- `com.synthetic.readiness.ReadinessRulesTest.acceptedExceptionNeedsApprover`
- `com.synthetic.readiness.ReadinessRulesTest.completeNeedsTimestamp`
- `com.synthetic.readiness.ReadinessRulesTest.acceptedExceptionNeedsRationale`
- `com.synthetic.readiness.ReadinessRulesTest.attachedEvidenceWithoutOwnerDoesNotComplete`
- `com.synthetic.readiness.ReadinessRulesTest.percentageDoesNotGoBelowZero`
- `com.synthetic.readiness.ReadinessRulesTest.noBlockersSuggestGo`
- `com.synthetic.readiness.ReadinessRulesTest.percentageDoesNotExceedOneHundred`
- `com.synthetic.readiness.ReadinessRulesTest.criticalBlockerTakesPriorityOverOtherBlockers`
- `com.synthetic.readiness.ReadinessRulesTest.zeroMandatoryActionsAreFullyReady`
- `com.synthetic.readiness.ReadinessRulesTest.highBlockerSuggestsConditionalGo`
- `com.synthetic.readiness.ReadinessRulesTest.completeNeedsOwnerRecorded`
- `com.synthetic.readiness.ReadinessRulesTest.completeNeedsEvidence`
- `com.synthetic.readiness.ReadinessRulesTest.unacceptedExceptionDoesNotComplete`
- `com.synthetic.readiness.ReadinessRulesTest.criticalBlockerSuggestsNoGo`
- `com.synthetic.readiness.SourceGuardTest.sourceTemplatesDocsAndTestsStayFirmNeutralAndWordFree`
- `com.synthetic.readiness.summary.SummaryServiceTest.modelDraftUsesInjectedFakeAndIsMarkedDraft`
- `com.synthetic.readiness.WorkbenchIntegrationTest.onlyReadinessLeadCanPostHtmlDecision`
- `com.synthetic.readiness.WorkbenchIntegrationTest.actionCompletionWithEvidencePersistsAndAudits`
- `com.synthetic.readiness.WorkbenchIntegrationTest.stabilisationQueueEvictsOldestAtConfiguredCap`
- `com.synthetic.readiness.WorkbenchIntegrationTest.readinessShowsNumeratorAndDenominator`
- `com.synthetic.readiness.WorkbenchIntegrationTest.auditApiHasNoUpdateRoute`
- `com.synthetic.readiness.WorkbenchIntegrationTest.materialDecisionWritesAuditEvent`
- `com.synthetic.readiness.WorkbenchIntegrationTest.initialDataIncludesRequiredSyntheticActionCount`
- `com.synthetic.readiness.WorkbenchIntegrationTest.readinessLeadCanRecordHumanDecision`
- `com.synthetic.readiness.WorkbenchIntegrationTest.onlyReadinessLeadCanRecordDecision`
- `com.synthetic.readiness.WorkbenchIntegrationTest.kafkaAcceptedPaymentDoesNotEnterExceptionQueue`
- `com.synthetic.readiness.WorkbenchIntegrationTest.onlyControlPartnerCanSignOff`
- `com.synthetic.readiness.WorkbenchIntegrationTest.kafkaRoutesRejectedPaymentIntoStabilisationQueue`
- `com.synthetic.readiness.WorkbenchIntegrationTest.conditionalGoNeedsCondition`
- `com.synthetic.readiness.WorkbenchIntegrationTest.summaryIsDraftUntilReadinessLeadApproves`
- `com.synthetic.readiness.WorkbenchIntegrationTest.controlPartnerCanSignOff`
- `com.synthetic.readiness.WorkbenchIntegrationTest.syntheticPaymentStatusEventsDefaultToSyntheticLabel`
- `com.synthetic.readiness.WorkbenchIntegrationTest.initialDataHasNineUatScenarios`
- `com.synthetic.readiness.WorkbenchIntegrationTest.dashboardShowsFiveImpactedTeams`
- `com.synthetic.readiness.WorkbenchIntegrationTest.decisionIsInitiallyAbsent`
- `com.synthetic.readiness.WorkbenchIntegrationTest.demoEventProducerPublishesOnlyAfterReleaseIsLive`
- `com.synthetic.readiness.WorkbenchIntegrationTest.releaseRegisterFiltersByStatusRegionAndDate`
- `com.synthetic.readiness.WorkbenchIntegrationTest.flywaySeedsOneSyntheticRelease`
- `com.synthetic.readiness.WorkbenchIntegrationTest.initialDataHasSevenDefectsIncludingCritical`
- `com.synthetic.readiness.WorkbenchIntegrationTest.executiveViewerCanReadDashboard`
- `com.synthetic.readiness.WorkbenchIntegrationTest.dashboardRegionFilterChangesSeededReadinessAndDefects`
- `com.synthetic.readiness.WorkbenchIntegrationTest.executiveViewerCannotApproveSummary`
- `com.synthetic.readiness.WorkbenchIntegrationTest.criticalDefectBlocksGoSuggestion`
- `com.synthetic.readiness.WorkbenchIntegrationTest.actionCannotBeCompletedWithoutEvidence`
- `com.synthetic.readiness.WorkbenchIntegrationTest.dashboardIncludesCriticalAndHighDefectAges`
- `com.synthetic.readiness.WorkbenchIntegrationTest.demoEventProducerIsDisabledByDefault`
- `com.synthetic.readiness.WorkbenchIntegrationTest.executiveViewerCannotCreateAction`
- `com.synthetic.readiness.WorkbenchIntegrationTest.dashboardCanFilterReleaseRegion`
- `com.synthetic.readiness.WorkbenchIntegrationTest.dashboardPageRendersPhaseTwoSummaryControls`
- `com.synthetic.readiness.WorkbenchIntegrationTest.auditApiHasNoDeleteRoute`

Test inventory contains 68 test methods; full `mvn verify` reports 70 invocations including parameterized cases.

## Concurrency & performance (Nov 2026) — A0–A1

Summary (50 concurrent writers on one `readiness_action` row; synthetic data, laptop benchmark):

| Variant | Outcome | Lost updates | Failed callers |
|---|---|---:|---:|
| No locking (red baseline, 1 run) | final `update_count` 5/50 | 45 | 0 (silent loss) |
| Optimistic `@Version`, 5 attempts, exponential jitter (pure, gate off) | exhausted 107–125 of 250 calls per invocation (43–50%), 4 invocations | 0 | 107–125 per 250 (rejected loudly; 409 via the API) |
| Optimistic + opt-in single-JVM retry gate | 0 exhausted, 3 invocations | 0 | 0 |
| Pessimistic `PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`) | 0 failures, 4 invocations | 0 | 0 |

No speed ratio is claimed: pure optimistic batches completed less work than pessimistic ones.

- Scenario is synthetic and uses seeded synthetic readiness actions; it is a local prototype workload, not bank-scale evidence.
- A0 target: `readiness_action`, updated by `PATCH /api/actions/{id}` and the web action-completion form. The project has no Spring Data repository for this entity; `WorkbenchService` reads it through JPA `EntityManager`, and `ActionConcurrencyService` now owns concurrent writes. Integration tests already use Testcontainers PostgreSQL 17.6; there is no H2 substitution.
- Red baseline: with `@Version` and the version column removed, 50 writers ran once through the plain read-modify-write path; final `update_count` was **5/50** (expected 50, observed 5). The assertion failed as intended. This was run against Testcontainers PostgreSQL 17.6 on JDK 25.0.4.
- First versioned attempt: 50 writers, five attempts, flat 1–10 ms jitter; retries exhausted in run 1. The optimistic version check was working, and an exhausted writer propagated through `future.get()`.
- Implementation: JPA `@Version` in V5; five total attempts; exponential jitter; warn log and HTTP 409 on exhaustion. The optional 64-stripe fair in-process retry gate is controlled by `READINESS_ACTION_OPTIMISTIC_RETRY_GATE_ENABLED` and defaults to `false`. When enabled, it mitigates retry stampedes only within one JVM; it does not coordinate application instances. The database version check remains the cross-instance concurrency control. The comparison path uses `PESSIMISTIC_WRITE` (`SELECT FOR UPDATE`); `update_count` is the lost-update assertion.
- Historical gated variant: the earlier three warmed invocations used optimistic updates plus the in-process retry gate. These measurements are superseded for the pure optimistic comparison and are retained only as mitigation evidence:

  | Invocation | Optimistic + gate median (ms) | Pessimistic median (ms) | Optimistic range (ms) | Pessimistic range (ms) | Retries | Exhausted |
  |---|---:|---:|---:|---:|---:|---:|
  | 1 | 314.256 | 159.430 | 268.641–601.937 | 122.319–195.339 | 224 | 0 |
  | 2 (`verify`) | 339.021 | 182.600 | 253.204–406.196 | 134.131–206.739 | 218 | 0 |
  | 3 | 515.631 | 220.258 | 426.486–575.326 | 192.763–315.121 | 225 | 0 |

  Their medians were optimistic + gate **339.021 ms** (314.256–515.631) and pessimistic **182.600 ms** (159.430–220.258). These are local synthetic-workload observations, not the pure optimistic baseline.
- Pure optimistic comparison: retry gate disabled; one 50-writer warm-up followed by five alternating measured pairs per invocation. All three invocations exhausted optimistic retries:

  ```text
  CONCURRENCY_BENCHMARK writers=50 runsPerMode=5 warmups=1 order=OP,PO,OP,PO,OP optimisticVariant=pure-optimistic-gate-disabled optimisticMedianMs=258.346 optimisticRangeMs=215.059-331.469 optimisticRetries=730 optimisticConflicts=851 optimisticExhaustedWriters=121 optimisticFailedWriters=121 warmupExhaustedWriters=27 warmupFailedWriters=27 pessimisticMedianMs=216.251 pessimisticRangeMs=155.228-421.665 pessimisticRetries=0 pessimisticConflicts=0 pessimisticExhaustedWriters=0 pessimisticFailedWriters=0 successfulUpdates=379
  CONCURRENCY_BENCHMARK writers=50 runsPerMode=5 warmups=1 order=OP,PO,OP,PO,OP optimisticVariant=pure-optimistic-gate-disabled optimisticMedianMs=369.697 optimisticRangeMs=305.672-504.317 optimisticRetries=724 optimisticConflicts=846 optimisticExhaustedWriters=122 optimisticFailedWriters=122 warmupExhaustedWriters=26 warmupFailedWriters=26 pessimisticMedianMs=267.113 pessimisticRangeMs=235.919-347.354 pessimisticRetries=0 pessimisticConflicts=0 pessimisticExhaustedWriters=0 pessimisticFailedWriters=0 successfulUpdates=378
  CONCURRENCY_BENCHMARK writers=50 runsPerMode=5 warmups=1 order=OP,PO,OP,PO,OP optimisticVariant=pure-optimistic-gate-disabled optimisticMedianMs=480.365 optimisticRangeMs=464.832-730.516 optimisticRetries=737 optimisticConflicts=862 optimisticExhaustedWriters=125 optimisticFailedWriters=125 warmupExhaustedWriters=25 warmupFailedWriters=25 pessimisticMedianMs=399.285 pessimisticRangeMs=354.494-481.017 pessimisticRetries=0 pessimisticConflicts=0 pessimisticExhaustedWriters=0 pessimisticFailedWriters=0 successfulUpdates=375
  ```

  Median of the three invocation medians: pure optimistic **369.697 ms** (258.346–480.365), pessimistic **267.113 ms** (216.251–399.285). The optimistic batches did not complete all 250 updates, so their elapsed times are not an equal-work speed comparison. The reliable finding is repeated exhaustion without the gate; do not state a speed ratio.
- The three targeted gate-disabled benchmark invocations exhausted optimistic retries and failed the old zero-exhaustion assertion. After changing the test to assert correctness instead, full `mvn verify` passed with **70 tests, 0 failures, 0 errors, 0 skipped**. The full verify benchmark was a fourth, separate confirmation run:

  ```text
  CONCURRENCY_BENCHMARK writers=50 runsPerMode=5 warmups=1 order=OP,PO,OP,PO,OP optimisticVariant=pure-optimistic-gate-disabled optimisticMedianMs=288.003 optimisticRangeMs=271.013-316.971 optimisticRetries=676 optimisticConflicts=783 optimisticExhaustedWriters=107 optimisticFailedWriters=107 warmupExhaustedWriters=27 warmupFailedWriters=27 pessimisticMedianMs=250.303 pessimisticRangeMs=220.693-331.153 pessimisticRetries=0 pessimisticConflicts=0 pessimisticExhaustedWriters=0 pessimisticFailedWriters=0 successfulUpdates=393 databaseUpdateCount=393
  ```

  This confirms `update_count` equalled acknowledged successful writes, while exhausted optimistic callers were reported as failures and no pessimistic writer failed. The test does not assert a specific optimistic exhaustion count.
- Measurement lesson: earlier comparisons always ran optimistic first and varied materially across invocations. Treat those comparisons as cold-start/order biased and exclude their ratios from professional-profile claims. The harness now uses one unmeasured 50-writer warm-up and alternates optimistic/pessimistic order across five measured pairs. The pure optimistic runs omit the optional single-JVM retry gate; do not conflate these results with the gated mitigation variant.
- ADR: `docs/adr/ADR-001-readiness-action-locking.md`. Diagrams: `docs/diagrams/readiness-action-optimistic-lock.md` and `docs/diagrams/readiness-action-concurrency-benchmark.md`.
- Back-off: five total attempts; after each of the first four conflicts the delay cap doubles (2, 4, 8, 16 ms) and the actual delay is drawn uniformly from half the cap to the full cap (1–2, 2–4, 4–8, 8–16 ms).
- Environment: Intel Core i7-1165G7 (4 cores / 8 threads, 2.80 GHz), 16 GB RAM, Windows 11; JDK Eclipse Temurin 25.0.4.1 (25.0.4+101); Maven 3.9.11; PostgreSQL 17.6-alpine via Testcontainers on Docker Desktop 4.96.3 (WSL2, VM memory 8 GB); HikariCP pool 10 (default). Dataset: one synthetic `readiness_action` row per benchmark, created and deleted by the test.
- Authorship: the concurrency test, red baseline, `@Version` and `PESSIMISTIC_WRITE` fixes, retry/back-off, opt-in retry gate, 409 mapping, benchmark harness, ADR-001 and the two diagrams were hand-coded/written by Kakali Bandyopadhyay. AI assistance (Claude): A0 target analysis, code review comments, and formatting of this FINDINGS section.

## Changed and added files

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/img/dashboard.png`
- `docs/img/decision-page.png`
- `docs/img/release-workspace.png`
- `docs/img/uat-workbench.png`
- `src/main/java/com/synthetic/readiness/ApiController.java`
- `src/main/java/com/synthetic/readiness/ApiErrors.java`
- `src/main/java/com/synthetic/readiness/SecurityConfig.java`
- `src/main/java/com/synthetic/readiness/WebController.java`
- `src/main/java/com/synthetic/readiness/WorkbenchApplication.java`
- `src/main/java/com/synthetic/readiness/release/ReleaseRecord.java`
- `src/main/java/com/synthetic/readiness/release/ReleaseService.java`
- `src/main/java/com/synthetic/readiness/readiness/Impact.java`
- `src/main/java/com/synthetic/readiness/readiness/ReadinessAction.java`
- `src/main/java/com/synthetic/readiness/readiness/WorkbenchService.java` (moved from the root package)
- `src/main/java/com/synthetic/readiness/uat/Defect.java`
- `src/main/java/com/synthetic/readiness/uat/Retest.java`
- `src/main/java/com/synthetic/readiness/uat/UatScenario.java`
- `src/main/java/com/synthetic/readiness/uat/UatService.java`
- `src/main/java/com/synthetic/readiness/control/ControlReview.java`
- `src/main/java/com/synthetic/readiness/control/ControlService.java`
- `src/main/java/com/synthetic/readiness/decision/DecisionCondition.java`
- `src/main/java/com/synthetic/readiness/decision/DecisionService.java`
- `src/main/java/com/synthetic/readiness/decision/ReleaseDecision.java`
- `src/main/java/com/synthetic/readiness/audit/AuditEvent.java`
- `src/main/java/com/synthetic/readiness/audit/AuditService.java`
- `src/main/java/com/synthetic/readiness/dashboard/DashboardService.java`
- `src/main/java/com/synthetic/readiness/stabilisation/PaymentStatusEvent.java`
- `src/main/java/com/synthetic/readiness/stabilisation/PaymentStatusEventProducer.java`
- `src/main/java/com/synthetic/readiness/stabilisation/PaymentStatusEventConsumer.java`
- `src/main/java/com/synthetic/readiness/stabilisation/StabilisationEventConfiguration.java`
- `src/main/java/com/synthetic/readiness/stabilisation/StabilisationException.java`
- `src/main/java/com/synthetic/readiness/stabilisation/StabilisationService.java`
- `src/main/java/com/synthetic/readiness/summary/ExecutiveSummary.java`
- `src/main/java/com/synthetic/readiness/summary/ExecutiveSummaryService.java`
- `src/main/java/com/synthetic/readiness/summary/OpenAiSummaryTextClient.java`
- `src/main/java/com/synthetic/readiness/summary/SummaryTextClient.java`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V3__region_specific_seed.sql`
- `src/test/java/com/synthetic/readiness/SourceGuardTest.java`
- `src/test/java/com/synthetic/readiness/SummaryServiceTest.java` (package moved to `summary`)
- `src/test/java/com/synthetic/readiness/WorkbenchIntegrationTest.java`

Removed/moved out of the old combined files: `src/main/java/com/synthetic/readiness/DomainEntities.java`, `src/main/java/com/synthetic/readiness/SummaryService.java`, and `src/main/java/com/synthetic/readiness/KafkaPhaseTwo.java`. Kafka topic setup, producer, and consumer now live in the stabilisation package.

`pom.xml` and `docker-compose.yml` were not changed. No commit or push was made.

## Limitations and decisions

- Region ownership was not specified per seed row, so a simple deterministic split was chosen: actions 1–6 North, 7–11 Central, 12–16 South; impacts split North 1–4, Central 5–9, South 10; defects were distributed by explicit IDs in V3. Controls and decisions remain release-wide.
- Audit append-only behavior is enforced at the application API/service surface; direct database access can still alter rows.
- The browser walkthrough records a Conditional Go that changes the release to `LIVE`; event production remains disabled unless `DEMO_EVENTS_ENABLED=true` is explicitly set.
- `mvn clean verify` was attempted but could not download the uncached Maven clean plugin because access to the local Maven repository was denied. The successful final full-suite command was `mvn verify`.

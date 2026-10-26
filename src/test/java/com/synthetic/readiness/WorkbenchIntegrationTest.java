package com.synthetic.readiness;

import com.synthetic.readiness.release.*;
import com.synthetic.readiness.readiness.*;
import com.synthetic.readiness.uat.*;
import com.synthetic.readiness.control.*;
import com.synthetic.readiness.decision.*;
import com.synthetic.readiness.audit.*;
import com.synthetic.readiness.stabilisation.*;
import com.synthetic.readiness.summary.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import org.awaitility.Awaitility;
import java.time.Duration;
import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers @SpringBootTest @AutoConfigureMockMvc @Transactional
class WorkbenchIntegrationTest {
    @Container static PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17.6-alpine");
    @Container static KafkaContainer kafka=new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",postgres::getJdbcUrl);p.add("spring.datasource.username",postgres::getUsername);p.add("spring.datasource.password",postgres::getPassword);p.add("spring.kafka.bootstrap-servers",kafka::getBootstrapServers);}
    @Autowired MockMvc mvc;
    @Autowired WorkbenchService service;
    @Autowired KafkaTemplate<String,String> kafkaTemplate;
    @Autowired StabilisationService stabilisation;
    @Autowired ExecutiveSummaryService summaries;
    @Autowired ApplicationContext applicationContext;

    @Test void flywaySeedsOneSyntheticRelease() throws Exception { mvc.perform(get("/api/releases")).andExpect(status().isOk()).andExpect(jsonPath("$[0].synthetic").value(true)); }
    @Test void readinessShowsNumeratorAndDenominator() throws Exception { mvc.perform(get("/api/releases/1/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.denominator").value(13)).andExpect(jsonPath("$.numerator").value(2)); }
    @Test void criticalDefectBlocksGoSuggestion() throws Exception { mvc.perform(get("/api/releases/1/readiness")).andExpect(jsonPath("$.suggestedStatus").value("No-Go")); }
    @Test void dashboardCanFilterReleaseRegion() throws Exception { mvc.perform(get("/api/releases/1/dashboard").param("region","North")).andExpect(status().isOk()).andExpect(jsonPath("$.region").value("North")); }
    @Test void dashboardRegionFilterChangesSeededReadinessAndDefects() throws Exception {
        mvc.perform(get("/api/releases/1/dashboard").param("region","North")).andExpect(status().isOk()).andExpect(jsonPath("$.totalMandatory").value(6)).andExpect(jsonPath("$.completedMandatory").value(2)).andExpect(jsonPath("$.openCritical").value(1)).andExpect(jsonPath("$.teams.length()").value(2));
        mvc.perform(get("/api/releases/1/dashboard").param("region","Central")).andExpect(status().isOk()).andExpect(jsonPath("$.totalMandatory").value(4)).andExpect(jsonPath("$.completedMandatory").value(0)).andExpect(jsonPath("$.openCritical").value(0)).andExpect(jsonPath("$.teams.length()").value(3));
    }
    @Test void demoEventProducerIsDisabledByDefault() { org.junit.jupiter.api.Assertions.assertFalse(applicationContext.containsBean("syntheticPaymentProducer")); }
    @Test void demoEventProducerPublishesOnlyAfterReleaseIsLive() {
        KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class); ObjectMapper json=mock(ObjectMapper.class); WorkbenchService workbench=mock(WorkbenchService.class);
        ReleaseRecord release=new ReleaseRecord(); release.id=1L; release.lifecycleStatus="IN_READINESS"; when(workbench.release(1)).thenReturn(release);
        PaymentStatusEventProducer producer=new PaymentStatusEventProducer(kafka,json,workbench);
        producer.publishAfterRecordedGoLive(); verifyNoInteractions(kafka);
        release.lifecycleStatus="LIVE"; producer.publishAfterRecordedGoLive(); verify(kafka).send(eq("payment-status-events"),anyString(),any());
    }
    @Test void stabilisationQueueEvictsOldestAtConfiguredCap() {
        org.springframework.test.util.ReflectionTestUtils.setField(stabilisation,"maxStoredEvents",2);
        for(int n=0;n<3;n++) { String id="IT-CAP-"+n; stabilisation.route(new PaymentStatusEvent(id,1,"SYNTH-CAP-"+n,"REJECTED",LocalDateTime.now().plusSeconds(n),"Payment review",true)); }
        java.util.List<StabilisationException> retained=stabilisation.open(1).stream().filter(x->x.eventId.startsWith("IT-CAP-")).toList();
        org.junit.jupiter.api.Assertions.assertEquals(2,retained.size());
        org.junit.jupiter.api.Assertions.assertFalse(retained.stream().anyMatch(x->"IT-CAP-0".equals(x.eventId)));
    }
    @Test void syntheticPaymentStatusEventsDefaultToSyntheticLabel() { org.junit.jupiter.api.Assertions.assertTrue(new PaymentStatusEvent("IT-SYNTHETIC",1,"SYNTH-PAY", "ACCEPTED",LocalDateTime.now(),"Payment review",null).synthetic()); }
    @Test void releaseRegisterFiltersByStatusRegionAndDate() throws Exception { mvc.perform(get("/api/releases").param("status","IN_READINESS").param("region","North").param("from","2026-12-01").param("to","2026-12-30")).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(1))); }
    @Test void dashboardIncludesCriticalAndHighDefectAges() throws Exception { mvc.perform(get("/api/releases/1/dashboard")).andExpect(status().isOk()).andExpect(jsonPath("$.criticalAndHigh.length()").value(3)); }
    @Test void dashboardShowsFiveImpactedTeams() throws Exception { mvc.perform(get("/api/releases/1/dashboard")).andExpect(jsonPath("$.teams.length()").value(5)); }
    @Test void decisionIsInitiallyAbsent() throws Exception { mvc.perform(get("/api/releases/1/decisions")).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(0))); }
    @Test void executiveViewerCanReadDashboard() throws Exception { mvc.perform(get("/api/releases/1/dashboard").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("viewer").roles("EXECUTIVE_VIEWER"))).andExpect(status().isOk()); }
    @Test void executiveViewerCannotCreateAction() throws Exception { mvc.perform(post("/api/releases/1/actions").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("viewer").roles("EXECUTIVE_VIEWER")).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"PROCEDURE\",\"title\":\"Synthetic check\",\"owner\":\"Mira Sen\",\"dueDate\":\"2026-12-01\",\"mandatory\":true}" )).andExpect(status().isForbidden()); }
    @Test void actionCannotBeCompletedWithoutEvidence() throws Exception { mvc.perform(patch("/api/actions/3").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ops").roles("TEAM_OWNER")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETE\",\"completedBy\":\"ops\"}")).andExpect(status().isBadRequest()); }
    @Test void actionCompletionWithEvidencePersistsAndAudits() throws Exception { mvc.perform(patch("/api/actions/3").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ops").roles("TEAM_OWNER")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETE\",\"completedBy\":\"ops\",\"evidenceReference\":\"SYNTH-ACTION-REF\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETE"));org.junit.jupiter.api.Assertions.assertTrue(service.audit().stream().anyMatch(a->"ReadinessAction".equals(a.entityName)&&"UPDATE".equals(a.action))); }
    @Test void onlyReadinessLeadCanRecordDecision() throws Exception { mvc.perform(post("/api/releases/1/decisions").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ops").roles("TEAM_OWNER")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"GO\",\"rationale\":\"Synthetic\"}")).andExpect(status().isForbidden()); }
    @Test void readinessLeadCanRecordHumanDecision() throws Exception { mvc.perform(post("/api/releases/1/decisions").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("lead").roles("READINESS_LEAD")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"CONDITIONAL_GO\",\"rationale\":\"Illustrative\",\"conditions\":[{\"description\":\"Confirm routing\",\"owner\":\"Nila Roy\",\"dueDate\":\"2026-12-01\"}]}")).andExpect(status().isOk()).andExpect(jsonPath("$.recordedBy").value("lead")); }
    @Test void conditionalGoNeedsCondition() throws Exception { mvc.perform(post("/api/releases/1/decisions").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("lead").roles("READINESS_LEAD")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"CONDITIONAL_GO\",\"rationale\":\"Illustrative\",\"conditions\":[]}")).andExpect(status().isBadRequest()); }
    @Test void onlyControlPartnerCanSignOff() throws Exception { mvc.perform(post("/api/controls/1/signoff").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ops").roles("TEAM_OWNER"))).andExpect(status().isForbidden()); }
    @Test void controlPartnerCanSignOff() throws Exception { mvc.perform(post("/api/controls/1/signoff").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("control").roles("CONTROL_PARTNER"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SIGNED_OFF")); }
    @Test void auditApiHasNoUpdateRoute() throws Exception { mvc.perform(put("/api/audit/1").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isNotFound()); }
    @Test void auditApiHasNoDeleteRoute() throws Exception { mvc.perform(delete("/api/audit/1")).andExpect(status().isNotFound()); }
    @Test void materialDecisionWritesAuditEvent() throws Exception { service.decide(1,new WorkbenchService.DecisionInput("NO_GO","Synthetic rationale",java.util.List.of()),"lead");org.junit.jupiter.api.Assertions.assertTrue(service.audit().stream().anyMatch(a->"ReleaseDecision".equals(a.entityName)&&"RECORD".equals(a.action))); }
    @Test void initialDataIncludesRequiredSyntheticActionCount() { org.junit.jupiter.api.Assertions.assertEquals(16,service.actions(1).size()); }
    @Test void initialDataHasNineUatScenarios() { org.junit.jupiter.api.Assertions.assertEquals(9,service.scenarios(1).size()); }
    @Test void initialDataHasSevenDefectsIncludingCritical() { org.junit.jupiter.api.Assertions.assertEquals(7,service.defects(1).size()); }
    @Test void kafkaRoutesRejectedPaymentIntoStabilisationQueue() throws Exception {String id="IT-REJECTED-"+java.util.UUID.randomUUID();kafkaTemplate.send("payment-status-events","SYNTH-PAY-IT", "{\"eventId\":\""+id+"\",\"releaseId\":1,\"paymentReference\":\"SYNTH-PAY-IT\",\"status\":\"REJECTED\",\"occurredAt\":\"2026-10-26T12:00:00\",\"journeyStep\":\"Payment review\"}").get();Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(()->org.junit.jupiter.api.Assertions.assertTrue(stabilisation.open(1).stream().anyMatch(x->id.equals(x.eventId)&&"HIGH".equals(x.severity))));}
    @Test void kafkaAcceptedPaymentDoesNotEnterExceptionQueue() throws Exception {String id="IT-ACCEPTED-"+java.util.UUID.randomUUID();kafkaTemplate.send("payment-status-events","SYNTH-PAY-OK", "{\"eventId\":\""+id+"\",\"releaseId\":1,\"paymentReference\":\"SYNTH-PAY-OK\",\"status\":\"ACCEPTED\",\"occurredAt\":\"2026-10-26T12:00:00\",\"journeyStep\":\"Payment review\"}").get();Thread.sleep(1200);org.junit.jupiter.api.Assertions.assertFalse(stabilisation.open(1).stream().anyMatch(x->id.equals(x.eventId)));}
    @Test void summaryIsDraftUntilReadinessLeadApproves() throws Exception {var draft=summaries.generate(1,"DETERMINISTIC","lead");org.junit.jupiter.api.Assertions.assertEquals("DRAFT",draft.status);org.junit.jupiter.api.Assertions.assertNull(summaries.approved(1));summaries.approve(draft.id,"lead");org.junit.jupiter.api.Assertions.assertEquals("lead",summaries.approved(1).approvedBy);org.junit.jupiter.api.Assertions.assertTrue(service.audit().stream().anyMatch(a->"ExecutiveSummary".equals(a.entityName)&&"APPROVE".equals(a.action)));}
    @Test void executiveViewerCannotApproveSummary() throws Exception {var draft=summaries.generate(1,"DETERMINISTIC","lead");mvc.perform(post("/api/summaries/"+draft.id+"/approve").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("viewer").roles("EXECUTIVE_VIEWER"))).andExpect(status().isForbidden());}
    @Test void dashboardPageRendersPhaseTwoSummaryControls() throws Exception {mvc.perform(get("/releases/1/dashboard").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("lead").roles("READINESS_LEAD"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Generate deterministic draft"))).andExpect(content().string(org.hamcrest.Matchers.containsString("Stabilisation")));}
    @Test void onlyReadinessLeadCanPostHtmlDecision() throws Exception {mvc.perform(post("/releases/1/decision").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ops").roles("TEAM_OWNER")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).param("decision","NO_GO").param("rationale","Synthetic role check" )).andExpect(status().isForbidden());}
}


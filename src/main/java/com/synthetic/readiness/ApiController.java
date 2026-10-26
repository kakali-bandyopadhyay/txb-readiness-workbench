package com.synthetic.readiness;

import com.synthetic.readiness.readiness.WorkbenchService;

import com.synthetic.readiness.release.*;
import com.synthetic.readiness.readiness.*;
import com.synthetic.readiness.uat.*;
import com.synthetic.readiness.control.*;
import com.synthetic.readiness.decision.*;
import com.synthetic.readiness.audit.*;
import com.synthetic.readiness.stabilisation.*;
import com.synthetic.readiness.summary.*;
import com.synthetic.readiness.dashboard.*;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class ApiController {
    private final WorkbenchService service;
    private final StabilisationService stabilisation;
    private final ExecutiveSummaryService summaries;
    private final ReleaseService releaseService; private final UatService uatService; private final ControlService controlService;
    private final DecisionService decisionService; private final AuditService auditService; private final DashboardService dashboardService;
    public ApiController(WorkbenchService service, StabilisationService stabilisation, ExecutiveSummaryService summaries,
                         ReleaseService releaseService, UatService uatService, ControlService controlService,
                         DecisionService decisionService, AuditService auditService, DashboardService dashboardService) {
        this.service=service; this.stabilisation=stabilisation; this.summaries=summaries;
        this.releaseService=releaseService; this.uatService=uatService; this.controlService=controlService;
        this.decisionService=decisionService; this.auditService=auditService; this.dashboardService=dashboardService;
    }
    public record ActionCreate(String category,String title,String owner,LocalDate dueDate,boolean mandatory,String dependency) {}
    public record ActionUpdate(String owner,String dueDate,String status,String evidenceReference,String completedBy,String exceptionAccepted,String exceptionApprover,String exceptionRationale) {}
    public record ScenarioCreate(String name,String expectedResult,String participant,String executionStatus) {}
    public record ScenarioUpdate(String executionStatus,String participant) {}
    public record DefectCreate(String title,String severity,String owner) {}
    public record CloseRequest(String notes) {}
    @GetMapping("/releases") public List<ReleaseRecord> releases(@RequestParam(required=false) String status,@RequestParam(required=false) String region,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return releaseService.releases(status,region,from,to);}
    @GetMapping("/releases/{id}/actions") public List<ReadinessAction> actions(@PathVariable long id){return service.actions(id);}
    @PostMapping("/releases/{id}/actions") @PreAuthorize("hasAnyRole('TEAM_OWNER','READINESS_LEAD','PRODUCT_PARTNER')")
    public ReadinessAction createAction(@PathVariable long id,@Valid @RequestBody ActionCreate in,Authentication auth){ReadinessAction a=new ReadinessAction();a.releaseId=id;a.category=in.category();a.title=in.title();a.owner=in.owner();a.dueDate=in.dueDate();a.mandatory=in.mandatory();a.dependency=in.dependency();a.status="NOT_STARTED";return service.create(a,auth.getName(),role(auth),"ReadinessAction",a.title);}
    @PatchMapping("/actions/{id}") @PreAuthorize("hasAnyRole('TEAM_OWNER','READINESS_LEAD','PRODUCT_PARTNER')")
    public ReadinessAction updateAction(@PathVariable long id,@RequestBody ActionUpdate in,Authentication auth){ReadinessAction a=service.actionById(id);if(a==null)throw new NoSuchElementException();String before=summary(a);if(in.owner()!=null)a.owner=in.owner();if(in.dueDate()!=null)a.dueDate=LocalDate.parse(in.dueDate());if(in.evidenceReference()!=null)a.evidenceReference=in.evidenceReference();if(in.completedBy()!=null)a.completedBy=in.completedBy();if(in.exceptionAccepted()!=null)a.exceptionAccepted=Boolean.parseBoolean(in.exceptionAccepted());if(in.exceptionApprover()!=null)a.exceptionApprover=in.exceptionApprover();if(in.exceptionRationale()!=null)a.exceptionRationale=in.exceptionRationale();
        boolean canComplete=(a.completedBy!=null&&!a.completedBy.isBlank()&&a.evidenceReference!=null&&!a.evidenceReference.isBlank())||(a.exceptionAccepted&&a.exceptionApprover!=null&&!a.exceptionApprover.isBlank()&&a.exceptionRationale!=null&&!a.exceptionRationale.isBlank());
        if("COMPLETE".equalsIgnoreCase(in.status())&&!canComplete)throw new IllegalArgumentException("Completion requires owner completion and evidence, or an accepted exception with approver and rationale.");
        if(in.status()!=null)a.status=in.status().toUpperCase(Locale.ROOT);if("COMPLETE".equals(a.status))a.completedAt=LocalDateTime.now();return service.update(a,auth.getName(),role(auth),"ReadinessAction",id,before,summary(a));}
    @GetMapping("/releases/{id}/scenarios") public List<UatScenario> scenarios(@PathVariable long id){return uatService.scenarios(id);}
    @PostMapping("/releases/{id}/scenarios") @PreAuthorize("hasAnyRole('UAT_LEAD','READINESS_LEAD')") public UatScenario createScenario(@PathVariable long id,@RequestBody ScenarioCreate in,Authentication auth){UatScenario s=new UatScenario();s.releaseId=id;s.name=in.name();s.expectedResult=in.expectedResult();s.participant=in.participant();s.executionStatus=in.executionStatus()==null?"NOT_RUN":in.executionStatus();return service.create(s,auth.getName(),role(auth),"UatScenario",s.name);}
    @PatchMapping("/scenarios/{id}") @PreAuthorize("hasAnyRole('UAT_LEAD','READINESS_LEAD')") public UatScenario updateScenario(@PathVariable long id,@RequestBody ScenarioUpdate in,Authentication auth){UatScenario s=service.scenarioById(id);if(s==null)throw new NoSuchElementException("Scenario not found");String before=s.executionStatus;if(in.executionStatus()!=null)s.executionStatus=in.executionStatus().toUpperCase(Locale.ROOT);if(in.participant()!=null)s.participant=in.participant();return service.update(s,auth.getName(),role(auth),"UatScenario",id,before,s.executionStatus);}
    @GetMapping("/releases/{id}/defects") public List<Defect> defects(@PathVariable long id){return uatService.defects(id);}
    @GetMapping("/defects/{id}/retests") public List<Retest> retests(@PathVariable long id){return uatService.retests(id);}
    @PostMapping("/releases/{id}/defects") @PreAuthorize("hasAnyRole('UAT_LEAD','READINESS_LEAD')") public Defect createDefect(@PathVariable long id,@RequestBody DefectCreate in,Authentication auth){Defect d=new Defect();d.releaseId=id;d.title=in.title();d.severity=in.severity().toUpperCase(Locale.ROOT);d.owner=in.owner();d.status="OPEN";d.openedAt=LocalDateTime.now();return service.create(d,auth.getName(),role(auth),"Defect",d.title);}
    @PostMapping("/defects/{id}/close") @PreAuthorize("hasAnyRole('UAT_LEAD','READINESS_LEAD')") public Defect closeDefect(@PathVariable long id,@RequestBody CloseRequest in,Authentication auth){return uatService.closeDefect(id,auth.getName(),in.notes());}
    @GetMapping("/releases/{id}/controls") public List<ControlReview> controls(@PathVariable long id){return controlService.controls(id);}
    @PostMapping("/controls/{id}/signoff") @PreAuthorize("hasRole('CONTROL_PARTNER')") public ControlReview signoff(@PathVariable long id,Authentication auth){return controlService.signoff(id,auth.getName());}
    @GetMapping("/releases/{id}/readiness") public WorkbenchService.Readiness readiness(@PathVariable long id,@RequestParam(required=false) String region){return service.readiness(id,region);}
    @GetMapping("/releases/{id}/dashboard") public WorkbenchService.Dashboard dashboard(@PathVariable long id,@RequestParam(required=false) String region){return dashboardService.dashboard(id,region);}
    @GetMapping("/releases/{id}/stabilisation") public List<StabilisationException> stabilisation(@PathVariable long id){return stabilisation.open(id);}
    @GetMapping("/releases/{id}/summaries") public Map<String,Object> summaries(@PathVariable long id){return Map.of("approved",Optional.ofNullable(summaries.approved(id)),"drafts",summaries.all(id).stream().filter(s->"DRAFT".equals(s.status)).toList());}
    @PostMapping("/releases/{id}/summaries") public ExecutiveSummary generateSummary(@PathVariable long id,@RequestParam(defaultValue="DETERMINISTIC") String mode,Authentication auth){return summaries.generate(id,mode,auth.getName());}
    @PostMapping("/summaries/{id}/approve") @PreAuthorize("hasRole('READINESS_LEAD')") public ExecutiveSummary approveSummary(@PathVariable long id,Authentication auth){return summaries.approve(id,auth.getName());}
    @GetMapping("/releases/{id}/decisions") public List<ReleaseDecision> decisions(@PathVariable long id){return decisionService.decisions(id);}
    @PostMapping("/releases/{id}/decisions") @PreAuthorize("hasRole('READINESS_LEAD')") public ReleaseDecision decide(@PathVariable long id,@RequestBody WorkbenchService.DecisionInput in,Authentication auth){return decisionService.decide(id,in,auth.getName());}
    @PostMapping("/conditions/{id}/close") @PreAuthorize("hasRole('READINESS_LEAD')") public Map<String,String> closeCondition(@PathVariable long id,Authentication auth){decisionService.closeCondition(id,auth.getName());return Map.of("status","closed");}
    @GetMapping("/audit") public List<AuditEvent> audit(){return auditService.events();}
    private String role(Authentication auth){return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_","");}
    private String summary(ReadinessAction a){return "status="+a.status+", owner="+a.owner+", evidence="+a.evidenceReference+", exception="+a.exceptionAccepted;}
}




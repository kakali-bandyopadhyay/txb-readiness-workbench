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

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@Controller
public class WebController {
    private final WorkbenchService service;
    private final StabilisationService stabilisation;
    private final ExecutiveSummaryService summaries;
    private final ReleaseService releaseService; private final UatService uatService; private final ControlService controlService;
    private final DecisionService decisionService; private final AuditService auditService; private final DashboardService dashboardService;
    public WebController(WorkbenchService service, StabilisationService stabilisation, ExecutiveSummaryService summaries,
                         ReleaseService releaseService, UatService uatService, ControlService controlService,
                         DecisionService decisionService, AuditService auditService, DashboardService dashboardService) {
        this.service=service; this.stabilisation=stabilisation; this.summaries=summaries;
        this.releaseService=releaseService; this.uatService=uatService; this.controlService=controlService;
        this.decisionService=decisionService; this.auditService=auditService; this.dashboardService=dashboardService;
    }
    @GetMapping("/") public String home(@RequestParam(required=false) String status,@RequestParam(required=false) String region,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,Model m){m.addAttribute("releases",releaseService.releases(status,region,from,to));m.addAttribute("status",status);m.addAttribute("region",region);m.addAttribute("regions",List.of("North","Central","South"));return "releases";}
    @GetMapping("/releases/{id}") public String release(@PathVariable long id,Model m){m.addAttribute("release",releaseService.release(id));m.addAttribute("impacts",service.impacts(id));m.addAttribute("actions",service.actions(id));m.addAttribute("readiness",service.readiness(id));return "release";}
    @GetMapping("/releases/{id}/uat") public String uat(@PathVariable long id,Model m){m.addAttribute("release",releaseService.release(id));m.addAttribute("scenarios",uatService.scenarios(id));m.addAttribute("defects",uatService.defects(id));return "uat";}
    @GetMapping("/releases/{id}/controls") public String controls(@PathVariable long id,Model m){m.addAttribute("release",releaseService.release(id));m.addAttribute("controls",controlService.controls(id));return "controls";}
    @GetMapping("/releases/{id}/decision") public String decision(@PathVariable long id,Model m){m.addAttribute("release",releaseService.release(id));m.addAttribute("readiness",service.readiness(id));m.addAttribute("decisions",decisionService.decisions(id));List<ReleaseDecision>d=decisionService.decisions(id);m.addAttribute("conditions",d.isEmpty()?List.of():decisionService.conditions(d.getFirst().id));return "decision";}
    @GetMapping("/releases/{id}/dashboard") public String dashboard(@PathVariable long id,@RequestParam(required=false) String region,Model m){m.addAttribute("release",releaseService.release(id));m.addAttribute("dashboard",dashboardService.dashboard(id,region));m.addAttribute("summary",summaries.approved(id));m.addAttribute("summaryDrafts",summaries.all(id).stream().filter(s->"DRAFT".equals(s.status)).toList());m.addAttribute("regions",List.of("North","Central","South"));return "dashboard";}
    @GetMapping("/releases/{id}/stabilisation") public String stabilisation(@PathVariable long id,Model m){m.addAttribute("release",releaseService.release(id));m.addAttribute("exceptions",stabilisation.open(id).stream().map(x->new StabilisationRow(x,java.time.Duration.between(x.occurredAt,LocalDateTime.now()).toDays())).toList());return "stabilisation";}
    @PostMapping("/releases/{id}/summaries") public String generateSummary(@PathVariable long id,@RequestParam(defaultValue="DETERMINISTIC") String mode,Authentication auth){summaries.generate(id,mode,auth.getName());return "redirect:/releases/"+id+"/dashboard";}
    @PostMapping("/summaries/{id}/approve") @org.springframework.security.access.prepost.PreAuthorize("hasRole('READINESS_LEAD')") public String approveSummary(@PathVariable long id,Authentication auth){ExecutiveSummary s=summaries.approve(id,auth.getName());return "redirect:/releases/"+s.releaseId+"/dashboard";}
    public record StabilisationRow(StabilisationException exception,long ageDays){}
    @PostMapping("/actions/{id}/complete") public String complete(@PathVariable long id,@RequestParam String evidence,Authentication auth){ReadinessAction a=service.actionById(id);String before="status="+a.status; a.evidenceReference=evidence;a.completedBy=auth.getName();a.completedAt=LocalDateTime.now();a.status="COMPLETE";String role=auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_","");service.update(a,auth.getName(),role,"ReadinessAction",id,before,"status=COMPLETE, evidence="+evidence);return "redirect:/releases/"+a.releaseId;}
    @PostMapping("/defects/{id}/close") public String closeDefect(@PathVariable long id,@RequestParam String notes,Authentication auth){uatService.closeDefect(id,auth.getName(),notes);Defect d=service.defectById(id);return "redirect:/releases/"+d.releaseId+"/uat";}
    @PostMapping("/controls/{id}/signoff") public String signoff(@PathVariable long id,Authentication auth){ControlReview c=controlService.signoff(id,auth.getName());return "redirect:/releases/"+c.releaseId+"/controls";}
    @PostMapping("/releases/{id}/decision") @org.springframework.security.access.prepost.PreAuthorize("hasRole('READINESS_LEAD')") public String decide(@PathVariable long id,@RequestParam String decision,@RequestParam String rationale,@RequestParam(required=false) String condition,@RequestParam(required=false) String conditionOwner,@RequestParam(required=false) LocalDate conditionDue,Authentication auth){
        List<WorkbenchService.ConditionInput> cs=new ArrayList<>();if("CONDITIONAL_GO".equals(decision)&&condition!=null)cs.add(new WorkbenchService.ConditionInput(condition,conditionOwner,conditionDue));decisionService.decide(id,new WorkbenchService.DecisionInput(decision,rationale,cs),auth.getName());return "redirect:/releases/"+id+"/decision";
    }
    @PostMapping("/conditions/{id}/close") public String closeCondition(@PathVariable long id,Authentication auth){decisionService.closeCondition(id,auth.getName());DecisionCondition c=decisionService.conditionById(id);ReleaseDecision d=decisionService.decisionById(c.decisionId);return "redirect:/releases/"+d.releaseId+"/decision";}
}




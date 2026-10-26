package com.synthetic.readiness.readiness;

import com.synthetic.readiness.release.*;
import com.synthetic.readiness.readiness.*;
import com.synthetic.readiness.uat.*;
import com.synthetic.readiness.control.*;
import com.synthetic.readiness.decision.*;
import com.synthetic.readiness.audit.*;
import com.synthetic.readiness.stabilisation.*;
import com.synthetic.readiness.summary.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class WorkbenchService {
    @PersistenceContext EntityManager em;
    private final Clock clock = Clock.systemDefaultZone();

    public record ConditionInput(String description, String owner, LocalDate dueDate) {}
    public record DecisionInput(String decision, String rationale, List<ConditionInput> conditions) {}
    public record Blocker(String rule, String item, String detail) {}
    public record TeamReadiness(String team, long complete, long total, int percent) {}
    public record DefectAge(Long id, String title, String severity, String owner, long ageDays, String status) {}
    public record Dashboard(Long releaseId, String region, int readinessPercent, long completedMandatory, long totalMandatory,
        long openCritical, long openHigh, List<DefectAge> criticalAndHigh, List<ReadinessAction> overdueActions,
        List<ReadinessAction> dueSoonActions, List<TeamReadiness> teams, String decision, long openConditions, List<Blocker> blockers) {}
    public record Readiness(int percent, long numerator, long denominator, String suggestedStatus, List<Blocker> blockers) {}

    public List<ReleaseRecord> releases(String status, String region, LocalDate from, LocalDate to) {
        return em.createQuery("select r from ReleaseRecord r order by r.id", ReleaseRecord.class).getResultList().stream()
            .filter(r -> status == null || status.isBlank() || r.lifecycleStatus.equalsIgnoreCase(status))
            .filter(r -> region == null || region.isBlank() || Arrays.stream(r.regions.split(",\\s*")).anyMatch(x -> x.equalsIgnoreCase(region)))
            .filter(r -> from == null || !r.targetDate.isBefore(from)).filter(r -> to == null || !r.targetDate.isAfter(to)).toList();
    }
    public ReleaseRecord release(long id) { return em.find(ReleaseRecord.class, id); }
    public ReadinessAction actionById(long id) { return em.find(ReadinessAction.class,id); }
    public UatScenario scenarioById(long id) { return em.find(UatScenario.class,id); }
    public Defect defectById(long id) { return em.find(Defect.class,id); }
    public DecisionCondition conditionById(long id) { return em.find(DecisionCondition.class,id); }
    public ReleaseDecision decisionById(long id) { return em.find(ReleaseDecision.class,id); }
    public List<Impact> impacts(long releaseId) { return em.createQuery("select i from Impact i where i.releaseId=:id order by i.id", Impact.class).setParameter("id",releaseId).getResultList(); }
    public List<Impact> impacts(long releaseId, String region) { return impacts(releaseId).stream().filter(i -> matches(i.region, region)).toList(); }
    public List<ReadinessAction> actions(long releaseId) { return em.createQuery("select a from ReadinessAction a where a.releaseId=:id order by a.dueDate,a.id", ReadinessAction.class).setParameter("id",releaseId).getResultList(); }
    public List<ReadinessAction> actions(long releaseId, String region) { return actions(releaseId).stream().filter(a -> matches(a.region, region)).toList(); }
    public List<UatScenario> scenarios(long releaseId) { return em.createQuery("select s from UatScenario s where s.releaseId=:id order by s.id", UatScenario.class).setParameter("id",releaseId).getResultList(); }
    public List<Defect> defects(long releaseId) { return em.createQuery("select d from Defect d where d.releaseId=:id order by d.id", Defect.class).setParameter("id",releaseId).getResultList(); }
    public List<Defect> defects(long releaseId, String region) { return defects(releaseId).stream().filter(d -> matches(d.region, region)).toList(); }
    public List<ControlReview> controls(long releaseId) { return em.createQuery("select c from ControlReview c where c.releaseId=:id order by c.id", ControlReview.class).setParameter("id",releaseId).getResultList(); }
    public List<ReleaseDecision> decisions(long releaseId) { return em.createQuery("select d from ReleaseDecision d where d.releaseId=:id order by d.recordedAt desc", ReleaseDecision.class).setParameter("id",releaseId).getResultList(); }
    public List<DecisionCondition> conditions(long decisionId) { return em.createQuery("select c from DecisionCondition c where c.decisionId=:id order by c.id", DecisionCondition.class).setParameter("id",decisionId).getResultList(); }
    public List<AuditEvent> audit() { return em.createQuery("select a from AuditEvent a order by a.occurredAt desc", AuditEvent.class).getResultList(); }
    public List<Retest> retests(long defectId) { return em.createQuery("select r from Retest r where r.defectId=:id order by r.testedAt desc", Retest.class).setParameter("id",defectId).getResultList(); }

    public boolean actionComplete(ReadinessAction a) {
        boolean evidenceCompletion = a.completedBy != null && !a.completedBy.isBlank() && a.completedAt != null && a.evidenceReference != null && !a.evidenceReference.isBlank();
        boolean exceptionCompletion = a.exceptionAccepted && a.exceptionApprover != null && !a.exceptionApprover.isBlank() && a.exceptionRationale != null && !a.exceptionRationale.isBlank();
        return evidenceCompletion || exceptionCompletion;
    }
    public Readiness readiness(long releaseId) {
        return readiness(releaseId, null);
    }
    public Readiness readiness(long releaseId, String region) {
        List<ReadinessAction> mandatory = actions(releaseId, region).stream().filter(a -> a.mandatory).toList();
        long complete = mandatory.stream().filter(this::actionComplete).count();
        List<Blocker> blockers = new ArrayList<>();
        for (ReadinessAction a : mandatory) if (!actionComplete(a)) blockers.add(new Blocker("Mandatory action evidence rule", a.title, "Completion needs a recorded owner and evidence reference, or an accepted exception with approver and rationale."));
        for (Defect d : defects(releaseId, region)) if ("OPEN".equalsIgnoreCase(d.status) && "CRITICAL".equalsIgnoreCase(d.severity)) blockers.add(new Blocker("Critical defect gate", d.title, "Open critical defects block a Go suggestion."));
        for (Defect d : defects(releaseId, region)) if ("OPEN".equalsIgnoreCase(d.status) && "HIGH".equalsIgnoreCase(d.severity)) blockers.add(new Blocker("High defect condition", d.title, "Open high defects require named conditions for a conditional suggestion."));
        for (ControlReview c : controls(releaseId)) if (!"SIGNED_OFF".equalsIgnoreCase(c.status)) blockers.add(new Blocker("Control sign-off gate", c.controlName, "Control review is not signed off by a control partner."));
        int percent = completionPercent(complete, mandatory.size());
        String suggestion = suggestedStatus(blockers);
        return new Readiness(percent, complete, mandatory.size(), suggestion, List.copyOf(blockers));
    }
    public static int completionPercent(long completed,long total) { return total <= 0 ? 100 : (int)Math.round(100.0 * Math.max(0,Math.min(completed,total)) / total); }
    public static String suggestedStatus(List<Blocker> blockers) {
        return blockers.stream().anyMatch(b -> b.rule().equals("Critical defect gate")) ? "No-Go" : (blockers.isEmpty() ? "Go" : "Conditional Go");
    }
    public Dashboard dashboard(long releaseId, String region) {
        List<ReadinessAction> allActions = actions(releaseId, region);
        LocalDate today = LocalDate.now(clock), soon = today.plusDays(5);
        List<Defect> open = defects(releaseId, region).stream().filter(d -> "OPEN".equalsIgnoreCase(d.status)).toList();
        List<DefectAge> important = open.stream().filter(d -> Set.of("CRITICAL","HIGH").contains(d.severity.toUpperCase(Locale.ROOT)))
            .map(d -> new DefectAge(d.id,d.title,d.severity,d.owner,Duration.between(d.openedAt,LocalDateTime.now(clock)).toDays(),d.status)).toList();
        Map<String,long[]> teamCounts = new TreeMap<>();
        for (Impact i : impacts(releaseId, region)) {
            long[] counts = teamCounts.computeIfAbsent(i.team, k -> new long[2]); counts[1]++;
            boolean teamComplete = allActions.stream().filter(a -> a.owner.equals(i.owner) && a.mandatory).allMatch(this::actionComplete);
            if (teamComplete) counts[0]++;
        }
        List<TeamReadiness> teams = teamCounts.entrySet().stream().map(e -> new TeamReadiness(e.getKey(),e.getValue()[0],e.getValue()[1],e.getValue()[1] == 0 ? 100 : (int)Math.round(100.0*e.getValue()[0]/e.getValue()[1]))).toList();
        List<ReleaseDecision> last = decisions(releaseId);
        long openConditions = last.isEmpty() ? 0 : conditions(last.getFirst().id).stream().filter(c -> !c.closed).count();
        Readiness r = readiness(releaseId, region);
        return new Dashboard(releaseId,region,r.percent(),r.numerator(),r.denominator(),open.stream().filter(d->"CRITICAL".equalsIgnoreCase(d.severity)).count(),open.stream().filter(d->"HIGH".equalsIgnoreCase(d.severity)).count(),important,
            allActions.stream().filter(a->a.dueDate.isBefore(today)&&!actionComplete(a)).toList(),allActions.stream().filter(a->!a.dueDate.isBefore(today)&&!a.dueDate.isAfter(soon)&&!actionComplete(a)).toList(),teams,
            last.isEmpty()?"Not recorded":last.getFirst().decision,openConditions,r.blockers());
    }
    private boolean matches(String value, String region) { return region == null || region.isBlank() || region.equalsIgnoreCase(value); }
    @Transactional public <T> T create(T entity, String actor, String role, String name, String summary) { em.persist(entity); em.flush(); audit(actor,role,name,idOf(entity),"CREATE",null,summary); return entity; }
    @Transactional public <T> T update(T entity, String actor, String role, String name, long id, String before, String after) { T merged=em.merge(entity); audit(actor,role,name,id,"UPDATE",before,after); return merged; }
    @Transactional public void audit(String actor,String role,String entity,Long id,String action,String before,String after) {
        AuditEvent e=new AuditEvent(); e.actor=actor; e.actorRole=role; e.entityName=entity; e.entityId=id; e.action=action; e.beforeSummary=before; e.afterSummary=after; e.occurredAt=LocalDateTime.now(clock); em.persist(e);
    }
    @Transactional public ReleaseDecision decide(long releaseId, DecisionInput input, String actor) {
        if (input == null || !Set.of("GO","CONDITIONAL_GO","NO_GO").contains(input.decision().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Decision must be Go, Conditional Go, or No-Go.");
        if (input.rationale()==null || input.rationale().isBlank()) throw new IllegalArgumentException("Decision rationale is required.");
        if ("CONDITIONAL_GO".equalsIgnoreCase(input.decision())) {
            if (input.conditions()==null || input.conditions().isEmpty()) throw new IllegalArgumentException("Conditional Go needs at least one condition.");
            for (ConditionInput c:input.conditions()) if (c.description()==null||c.description().isBlank()||c.owner()==null||c.owner().isBlank()||c.dueDate()==null) throw new IllegalArgumentException("Each condition needs a description, owner, and due date.");
        }
        ReleaseDecision d=new ReleaseDecision(); d.releaseId=releaseId; d.decision=input.decision().replace('_',' '); d.rationale=input.rationale(); d.recordedBy=actor; d.recordedAt=LocalDateTime.now(clock); em.persist(d); em.flush();
        if (input.conditions()!=null) for (ConditionInput c:input.conditions()) { DecisionCondition x=new DecisionCondition(); x.decisionId=d.id; x.description=c.description(); x.owner=c.owner(); x.dueDate=c.dueDate(); x.closed=false; em.persist(x); }
        audit(actor,"READINESS_LEAD","ReleaseDecision",d.id,"RECORD",null,d.decision+": "+d.rationale);
        ReleaseRecord release=em.find(ReleaseRecord.class,releaseId);
        if(release!=null){String before=release.lifecycleStatus;release.lifecycleStatus="NO_GO".equalsIgnoreCase(input.decision())?"IN_READINESS":"LIVE";if(!Objects.equals(before,release.lifecycleStatus))audit(actor,"READINESS_LEAD","ReleaseRecord",releaseId,"LIFECYCLE_CHANGE",before,release.lifecycleStatus);}
        return d;
    }
    @Transactional public void closeCondition(long id,String actor) { DecisionCondition c=em.find(DecisionCondition.class,id); if(c==null)throw new NoSuchElementException("Condition not found"); String before="closed="+c.closed; c.closed=true; audit(actor,"READINESS_LEAD","DecisionCondition",id,"CLOSE",before,"closed=true"); }
    @Transactional public ControlReview signoff(long id,String actor) { ControlReview c=em.find(ControlReview.class,id); if(c==null)throw new NoSuchElementException("Control review not found"); String before=c.status; c.status="SIGNED_OFF"; c.signedOffBy=actor; c.signedOffAt=LocalDateTime.now(clock); audit(actor,"CONTROL_PARTNER","ControlReview",id,"SIGN_OFF",before,c.status+" by "+actor); return c; }
    @Transactional public Defect closeDefect(long id,String actor,String notes) { Defect d=em.find(Defect.class,id); if(d==null)throw new NoSuchElementException("Defect not found"); String before=d.status; d.status="CLOSED"; d.closedAt=LocalDateTime.now(clock); audit(actor,"UAT_LEAD","Defect",id,"CLOSE",before,"CLOSED"); Retest r=new Retest();r.defectId=id;r.testedAt=LocalDateTime.now(clock);r.tester=actor;r.result="PASSED";r.notes=notes;em.persist(r);return d; }
    private Long idOf(Object o) { try { return (Long)o.getClass().getField("id").get(o); } catch(Exception e) { throw new IllegalArgumentException(e); } }
}



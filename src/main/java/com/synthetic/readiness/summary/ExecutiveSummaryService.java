package com.synthetic.readiness.summary;

import com.synthetic.readiness.readiness.WorkbenchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class ExecutiveSummaryService {
    @PersistenceContext EntityManager em;
    private final WorkbenchService workbench;
    private final SummaryTextClient model;
    public ExecutiveSummaryService(WorkbenchService workbench, SummaryTextClient model) { this.workbench = workbench; this.model = model; }
    public List<ExecutiveSummary> all(long releaseId) { return em.createQuery("select s from ExecutiveSummary s where s.releaseId=:id order by s.generatedAt desc", ExecutiveSummary.class).setParameter("id", releaseId).getResultList(); }
    public ExecutiveSummary approved(long releaseId) { List<ExecutiveSummary> matches = em.createQuery("select s from ExecutiveSummary s where s.releaseId=:id and s.status='APPROVED' order by s.approvedAt desc", ExecutiveSummary.class).setParameter("id", releaseId).setMaxResults(1).getResultList(); return matches.isEmpty() ? null : matches.getFirst(); }
    @Transactional public ExecutiveSummary generate(long releaseId, String mode, String actor) {
        WorkbenchService.Dashboard d = workbench.dashboard(releaseId, null);
        String facts = "readiness=" + d.readinessPercent() + "%; mandatory=" + d.completedMandatory() + "/" + d.totalMandatory() + "; openCritical=" + d.openCritical() + "; openHigh=" + d.openHigh() + "; decision=" + d.decision() + "; openConditions=" + d.openConditions() + "; blockers=" + d.blockers().size();
        String selected = mode == null ? "DETERMINISTIC" : mode.toUpperCase(Locale.ROOT); String summary;
        if ("MODEL".equals(selected)) { try { summary = "DRAFT: " + model.summarize(facts); } catch (Exception e) { throw new IllegalArgumentException("Could not generate model draft: " + e.getMessage(), e); } }
        else { selected = "DETERMINISTIC"; summary = "DRAFT: Readiness is " + d.readinessPercent() + "% (" + d.completedMandatory() + " of " + d.totalMandatory() + " mandatory actions). There are " + d.openCritical() + " open critical and " + d.openHigh() + " open high defects, " + d.blockers().size() + " recorded blockers, and " + d.openConditions() + " open conditions. Actual decision: " + d.decision() + "."; }
        ExecutiveSummary s = new ExecutiveSummary(); s.releaseId = releaseId; s.text = summary; s.mode = selected; s.status = "DRAFT"; s.generatedBy = actor; s.generatedAt = LocalDateTime.now(); em.persist(s); return s;
    }
    @Transactional public ExecutiveSummary approve(long id, String actor) { ExecutiveSummary s = em.find(ExecutiveSummary.class, id); if (s == null) throw new NoSuchElementException("Summary draft not found"); if (!"DRAFT".equals(s.status)) throw new IllegalArgumentException("Only a draft can be approved."); s.status = "APPROVED"; s.approvedBy = actor; s.approvedAt = LocalDateTime.now(); workbench.audit(actor, "READINESS_LEAD", "ExecutiveSummary", id, "APPROVE", "status=DRAFT", "status=APPROVED; mode=" + s.mode); return s; }
}

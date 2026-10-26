package com.synthetic.readiness.decision;

import com.synthetic.readiness.readiness.WorkbenchService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DecisionService {
    private final WorkbenchService workbench;
    public DecisionService(WorkbenchService workbench) { this.workbench = workbench; }
    public List<ReleaseDecision> decisions(long releaseId) { return workbench.decisions(releaseId); }
    public List<DecisionCondition> conditions(long decisionId) { return workbench.conditions(decisionId); }
    public ReleaseDecision decisionById(long id) { return workbench.decisionById(id); }
    public DecisionCondition conditionById(long id) { return workbench.conditionById(id); }
    public ReleaseDecision decide(long releaseId, WorkbenchService.DecisionInput input, String actor) { return workbench.decide(releaseId, input, actor); }
    public void closeCondition(long id, String actor) { workbench.closeCondition(id, actor); }
}

package com.synthetic.readiness.uat;

import com.synthetic.readiness.readiness.WorkbenchService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UatService {
    private final WorkbenchService workbench;
    public UatService(WorkbenchService workbench) { this.workbench = workbench; }
    public List<UatScenario> scenarios(long releaseId) { return workbench.scenarios(releaseId); }
    public List<Defect> defects(long releaseId) { return workbench.defects(releaseId); }
    public List<Retest> retests(long defectId) { return workbench.retests(defectId); }
    public Defect defectById(long id) { return workbench.defectById(id); }
    public Defect closeDefect(long id, String actor, String notes) { return workbench.closeDefect(id, actor, notes); }
}

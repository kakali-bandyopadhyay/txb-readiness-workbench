package com.synthetic.readiness.control;

import com.synthetic.readiness.readiness.WorkbenchService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ControlService {
    private final WorkbenchService workbench;
    public ControlService(WorkbenchService workbench) { this.workbench = workbench; }
    public List<ControlReview> controls(long releaseId) { return workbench.controls(releaseId); }
    public ControlReview signoff(long id, String actor) { return workbench.signoff(id, actor); }
}

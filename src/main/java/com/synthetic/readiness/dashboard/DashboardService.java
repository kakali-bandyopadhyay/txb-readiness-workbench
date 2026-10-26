package com.synthetic.readiness.dashboard;

import com.synthetic.readiness.readiness.WorkbenchService;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final WorkbenchService workbench;
    public DashboardService(WorkbenchService workbench) { this.workbench = workbench; }
    public WorkbenchService.Dashboard dashboard(long releaseId, String region) { return workbench.dashboard(releaseId, region); }
}

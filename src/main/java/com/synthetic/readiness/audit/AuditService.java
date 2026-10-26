package com.synthetic.readiness.audit;

import com.synthetic.readiness.readiness.WorkbenchService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final WorkbenchService workbench;
    public AuditService(WorkbenchService workbench) { this.workbench = workbench; }
    public List<AuditEvent> events() { return workbench.audit(); }
}

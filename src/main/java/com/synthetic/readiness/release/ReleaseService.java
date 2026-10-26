package com.synthetic.readiness.release;

import com.synthetic.readiness.readiness.WorkbenchService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReleaseService {
    private final WorkbenchService workbench;
    public ReleaseService(WorkbenchService workbench) { this.workbench = workbench; }
    public List<ReleaseRecord> releases(String status, String region, LocalDate from, LocalDate to) { return workbench.releases(status, region, from, to); }
    public ReleaseRecord release(long id) { return workbench.release(id); }
}

package com.synthetic.readiness.summary;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="executive_summary") public class ExecutiveSummary {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; @Column(length=2000) public String text; public String mode; public String status;
    public String generatedBy; public LocalDateTime generatedAt; public String approvedBy; public LocalDateTime approvedAt;
}


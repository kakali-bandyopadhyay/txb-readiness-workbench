package com.synthetic.readiness.readiness;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="readiness_action") public class ReadinessAction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; public String region; public String category; public String title; public String owner; public LocalDate dueDate; public boolean mandatory; public String dependency; public String evidenceReference;
    public String status; public String completedBy; public LocalDateTime completedAt; public boolean exceptionAccepted; public String exceptionApprover; public String exceptionRationale;
}


package com.synthetic.readiness.audit;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="audit_event") public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public String actor; public String actorRole; public String entityName; public Long entityId; public String action; @Column(length=4000) public String beforeSummary; @Column(length=4000) public String afterSummary; public LocalDateTime occurredAt;
}


package com.synthetic.readiness.decision;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="release_decision") public class ReleaseDecision {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; public String decision; public String rationale; public String recordedBy; public LocalDateTime recordedAt;
}


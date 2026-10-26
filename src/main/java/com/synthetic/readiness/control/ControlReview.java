package com.synthetic.readiness.control;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="control_review") public class ControlReview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; public String controlName; public String requiredChange; public String reviewer; public String status; public String evidenceReference; public String signedOffBy; public LocalDateTime signedOffAt;
}


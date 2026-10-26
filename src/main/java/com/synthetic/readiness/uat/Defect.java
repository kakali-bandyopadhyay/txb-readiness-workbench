package com.synthetic.readiness.uat;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="defect") public class Defect {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; public String region; public String title; public String severity; public String owner; public String status; public LocalDateTime openedAt; public LocalDateTime closedAt;
}


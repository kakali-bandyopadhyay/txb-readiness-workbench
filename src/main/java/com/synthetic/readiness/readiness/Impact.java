package com.synthetic.readiness.readiness;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="impact") public class Impact {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; public String region; public String team; public String journeyStep; public String impactType; public String owner; public String status;
}


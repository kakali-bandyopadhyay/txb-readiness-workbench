package com.synthetic.readiness.stabilisation;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="stabilisation_exception") public class StabilisationException {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public String eventId; public Long releaseId; public String paymentReference; public String status; public String severity;
    public LocalDateTime occurredAt; public LocalDateTime receivedAt; public String journeyStep; public String owner; public String nextAction; public String state="OPEN";
}


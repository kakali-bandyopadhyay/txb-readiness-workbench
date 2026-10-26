package com.synthetic.readiness.decision;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="decision_condition") public class DecisionCondition {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long decisionId; public String description; public String owner; public LocalDate dueDate; public boolean closed;
}


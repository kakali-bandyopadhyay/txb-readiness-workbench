package com.synthetic.readiness.uat;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="uat_scenario") public class UatScenario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long releaseId; public String name; @Column(length=1000) public String expectedResult; public String participant; public String executionStatus;
}


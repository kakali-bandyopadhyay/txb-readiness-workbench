package com.synthetic.readiness.uat;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="retest") public class Retest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public Long defectId; public LocalDateTime testedAt; public String tester; public String result; public String notes;
}


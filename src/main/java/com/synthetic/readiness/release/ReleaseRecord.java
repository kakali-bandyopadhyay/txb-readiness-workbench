package com.synthetic.readiness.release;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="release_record") public class ReleaseRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="is_synthetic", nullable=false) public boolean synthetic=true;
    public String name; @Column(length=1000) public String changeSummary; public LocalDate targetDate; public String regions; public String lifecycleStatus; public String owner;
}


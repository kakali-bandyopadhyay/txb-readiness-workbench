CREATE TABLE release_record (id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, change_summary VARCHAR(1000) NOT NULL, target_date DATE NOT NULL, regions VARCHAR(255) NOT NULL, lifecycle_status VARCHAR(40) NOT NULL, owner VARCHAR(120) NOT NULL);
CREATE TABLE impact (id BIGSERIAL PRIMARY KEY, release_id BIGINT NOT NULL REFERENCES release_record(id), team VARCHAR(120) NOT NULL, journey_step VARCHAR(180) NOT NULL, impact_type VARCHAR(120) NOT NULL, owner VARCHAR(120) NOT NULL, status VARCHAR(40) NOT NULL);
CREATE TABLE readiness_action (id BIGSERIAL PRIMARY KEY, release_id BIGINT NOT NULL REFERENCES release_record(id), category VARCHAR(60) NOT NULL, title VARCHAR(255) NOT NULL, owner VARCHAR(120) NOT NULL, due_date DATE NOT NULL, mandatory BOOLEAN NOT NULL, dependency VARCHAR(255), evidence_reference VARCHAR(255), status VARCHAR(40) NOT NULL, completed_by VARCHAR(120), completed_at TIMESTAMP, exception_accepted BOOLEAN NOT NULL DEFAULT FALSE, exception_approver VARCHAR(120), exception_rationale VARCHAR(1000));
CREATE TABLE uat_scenario (id BIGSERIAL PRIMARY KEY, release_id BIGINT NOT NULL REFERENCES release_record(id), name VARCHAR(255) NOT NULL, expected_result VARCHAR(1000) NOT NULL, participant VARCHAR(120) NOT NULL, execution_status VARCHAR(40) NOT NULL);
CREATE TABLE defect (id BIGSERIAL PRIMARY KEY, release_id BIGINT NOT NULL REFERENCES release_record(id), title VARCHAR(255) NOT NULL, severity VARCHAR(20) NOT NULL, owner VARCHAR(120) NOT NULL, status VARCHAR(40) NOT NULL, opened_at TIMESTAMP NOT NULL, closed_at TIMESTAMP);
CREATE TABLE retest (id BIGSERIAL PRIMARY KEY, defect_id BIGINT NOT NULL REFERENCES defect(id), tested_at TIMESTAMP NOT NULL, tester VARCHAR(120) NOT NULL, result VARCHAR(40) NOT NULL, notes VARCHAR(1000));
CREATE TABLE control_review (id BIGSERIAL PRIMARY KEY, release_id BIGINT NOT NULL REFERENCES release_record(id), control_name VARCHAR(255) NOT NULL, required_change VARCHAR(1000) NOT NULL, reviewer VARCHAR(120) NOT NULL, status VARCHAR(40) NOT NULL, evidence_reference VARCHAR(255), signed_off_by VARCHAR(120), signed_off_at TIMESTAMP);
CREATE TABLE release_decision (id BIGSERIAL PRIMARY KEY, release_id BIGINT NOT NULL REFERENCES release_record(id), decision VARCHAR(30) NOT NULL, rationale VARCHAR(1000) NOT NULL, recorded_by VARCHAR(120) NOT NULL, recorded_at TIMESTAMP NOT NULL);
CREATE TABLE decision_condition (id BIGSERIAL PRIMARY KEY, decision_id BIGINT NOT NULL REFERENCES release_decision(id), description VARCHAR(500) NOT NULL, owner VARCHAR(120) NOT NULL, due_date DATE NOT NULL, closed BOOLEAN NOT NULL DEFAULT FALSE);
CREATE TABLE audit_event (id BIGSERIAL PRIMARY KEY, actor VARCHAR(120) NOT NULL, actor_role VARCHAR(80) NOT NULL, entity_name VARCHAR(100) NOT NULL, entity_id BIGINT NOT NULL, action VARCHAR(80) NOT NULL, before_summary VARCHAR(4000), after_summary VARCHAR(4000), occurred_at TIMESTAMP NOT NULL);
ALTER TABLE release_record ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE impact ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE readiness_action ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE uat_scenario ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE defect ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE retest ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE control_review ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE release_decision ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE decision_condition ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE audit_event ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX idx_action_release ON readiness_action(release_id); CREATE INDEX idx_defect_release ON defect(release_id); CREATE INDEX idx_audit_time ON audit_event(occurred_at);
INSERT INTO release_record(name,change_summary,target_date,regions,lifecycle_status,owner) VALUES ('Real-time payments for corporate virtual accounts (synthetic)','Illustrative launch of payment status notifications for virtual accounts','2026-12-15','North, Central, South','IN_READINESS','Mira Sen (synthetic)');
INSERT INTO impact(release_id,team,journey_step,impact_type,owner,status) VALUES
(1,'Client onboarding','Configure virtual account','Procedure','Ari Das','IN_REVIEW'),(1,'Client onboarding','Confirm account readiness','Training','Ari Das','READY'),
(1,'Payments operations','Submit payment','Procedure','Nila Roy','IN_REVIEW'),(1,'Payments operations','Investigate payment status','Support model','Nila Roy','IN_PROGRESS'),
(1,'Liquidity and cash operations','Review intraday position','Technology dependency','Ira Bose','IN_PROGRESS'),(1,'Liquidity and cash operations','Resolve funding mismatch','Procedure','Ira Bose','READY'),
(1,'Client service','Answer payment enquiry','Training','Ravi Sen','IN_PROGRESS'),(1,'Client service','Escalate failed payment','Support model','Ravi Sen','IN_REVIEW'),
(1,'Reconciliations','Match payment status','Control','Mina Paul','IN_PROGRESS'),(1,'Reconciliations','Resolve unmatched item','Procedure','Mina Paul','READY');
INSERT INTO readiness_action(release_id,category,title,owner,due_date,mandatory,dependency,evidence_reference,status,completed_by,completed_at) VALUES
(1,'PROCEDURE','Approve virtual account operating guide','Ari Das','2026-11-10',true,null,'SYNTH-REF-001','COMPLETE','Ari Das','2026-10-20 10:00'),
(1,'TRAINING','Complete onboarding team briefing','Ari Das','2026-11-20',true,null,'SYNTH-REF-002','COMPLETE','Ari Das','2026-10-21 10:00'),
(1,'PROCEDURE','Publish payment investigation steps','Nila Roy','2026-11-05',true,null,null,'IN_PROGRESS',null,null),
(1,'SUPPORT_MODEL','Confirm out-of-hours support routing','Nila Roy','2026-11-15',true,null,null,'IN_PROGRESS',null,null),
(1,'TECHNOLOGY_DEPENDENCY','Validate status feed availability','Ira Bose','2026-11-18',true,'Synthetic event gateway',null,'IN_PROGRESS',null,null),
(1,'CONTROL','Approve reconciliation break handling','Mina Paul','2026-11-12',true,null,null,'IN_PROGRESS',null,null),
(1,'TRAINING','Brief client service team','Ravi Sen','2026-11-22',true,null,null,'NOT_STARTED',null,null),
(1,'PROCEDURE','Document liquidity escalation path','Ira Bose','2026-11-20',true,null,null,'NOT_STARTED',null,null),
(1,'SUPPORT_MODEL','Set client enquiry response guide','Ravi Sen','2026-11-25',false,null,null,'NOT_STARTED',null,null),
(1,'PROCEDURE','Verify daily payment reconciliation','Mina Paul','2026-11-28',true,null,null,'NOT_STARTED',null,null),
(1,'CONTROL','Review access and approval steps','Mina Paul','2026-11-14',true,null,null,'IN_PROGRESS',null,null),
(1,'TRAINING','Run operations scenario briefing','Nila Roy','2026-11-30',true,null,null,'NOT_STARTED',null,null),
(1,'TECHNOLOGY_DEPENDENCY','Confirm monitoring owner and alerts','Ira Bose','2026-11-26',false,null,null,'NOT_STARTED',null,null),
(1,'PROCEDURE','Reconcile rejected payment examples','Mina Paul','2026-12-01',true,null,null,'NOT_STARTED',null,null),
(1,'SUPPORT_MODEL','Confirm incident escalation contacts','Ravi Sen','2026-11-27',true,null,null,'NOT_STARTED',null,null),
(1,'CONTROL','Archive synthetic UAT evidence index','Ari Das','2026-12-02',false,null,null,'NOT_STARTED',null,null);
INSERT INTO uat_scenario(release_id,name,expected_result,participant,execution_status) VALUES
(1,'Accepted payment status','Accepted status visible in the synthetic client view','Nila Roy','PASSED'),(1,'Rejected payment status','Rejected status routes to operations review','Nila Roy','FAILED'),
(1,'Duplicate status event','Duplicate does not create a second client notification','Mina Paul','NOT_RUN'),(1,'Late status event','Late status is marked with its received time','Ravi Sen','NOT_RUN'),
(1,'Virtual account mismatch','Mismatch is held for manual review','Ari Das','PASSED'),(1,'Funding unavailable','Funding exception follows the liquidity escalation path','Ira Bose','NOT_RUN'),
(1,'Partial reconciliation','Unmatched item appears on the reconciliation list','Mina Paul','NOT_RUN'),(1,'Client enquiry','Support guide provides the correct status explanation','Ravi Sen','NOT_RUN'),
(1,'Access review','Only designated roles can approve a control change','Mina Paul','NOT_RUN');
INSERT INTO defect(release_id,title,severity,owner,status,opened_at) VALUES
(1,'Rejected event not routed to review','CRITICAL','Nila Roy','OPEN','2026-10-21 09:00'),(1,'Late status label unclear','HIGH','Ravi Sen','OPEN','2026-10-22 11:00'),
(1,'Duplicate event creates repeated row','MEDIUM','Mina Paul','OPEN','2026-10-23 12:00'),(1,'Funding example displays stale value','HIGH','Ira Bose','OPEN','2026-10-20 08:00'),
(1,'Operations note missing timestamp','LOW','Ari Das','OPEN','2026-10-24 08:00'),(1,'Status text wraps on narrow screen','LOW','Ravi Sen','OPEN','2026-10-25 14:00'),
(1,'Unmatched record lacks journey label','MEDIUM','Mina Paul','OPEN','2026-10-24 10:00');
INSERT INTO control_review(release_id,control_name,required_change,reviewer,status,evidence_reference) VALUES
(1,'Payment status authorisation','Confirm release approval pathway','Mina Paul','IN_REVIEW',null),
(1,'Reconciliation exception handling','Confirm unmatched item ownership','Mina Paul','IN_REVIEW',null),
(1,'Operational access review','Confirm role-based access is documented','Ari Das','PENDING',null),
(1,'Client communication controls','Confirm status explanations are approved','Ravi Sen','PENDING',null);

CREATE TABLE stabilisation_exception (
  id BIGSERIAL PRIMARY KEY, is_synthetic BOOLEAN NOT NULL DEFAULT TRUE, event_id VARCHAR(120) NOT NULL UNIQUE,
  release_id BIGINT NOT NULL REFERENCES release_record(id), payment_reference VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL, severity VARCHAR(20) NOT NULL, occurred_at TIMESTAMP NOT NULL,
  received_at TIMESTAMP NOT NULL, journey_step VARCHAR(180) NOT NULL, owner VARCHAR(120) NOT NULL,
  next_action VARCHAR(500) NOT NULL, state VARCHAR(30) NOT NULL DEFAULT 'OPEN'
);
CREATE TABLE executive_summary (
  id BIGSERIAL PRIMARY KEY, is_synthetic BOOLEAN NOT NULL DEFAULT TRUE,
  release_id BIGINT NOT NULL REFERENCES release_record(id), text VARCHAR(2000) NOT NULL,
  mode VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL, generated_by VARCHAR(120) NOT NULL,
  generated_at TIMESTAMP NOT NULL, approved_by VARCHAR(120), approved_at TIMESTAMP
);

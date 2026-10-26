ALTER TABLE impact ADD COLUMN region VARCHAR(30) NOT NULL DEFAULT 'North';
ALTER TABLE readiness_action ADD COLUMN region VARCHAR(30) NOT NULL DEFAULT 'North';
ALTER TABLE defect ADD COLUMN region VARCHAR(30) NOT NULL DEFAULT 'North';

UPDATE impact SET region = CASE WHEN id <= 4 THEN 'North' WHEN id <= 9 THEN 'Central' ELSE 'South' END;
UPDATE readiness_action SET region = CASE WHEN id <= 6 THEN 'North' WHEN id <= 11 THEN 'Central' ELSE 'South' END;
UPDATE defect SET region = CASE WHEN id IN (1,3) THEN 'North' WHEN id IN (2,5,7) THEN 'Central' ELSE 'South' END;

CREATE INDEX idx_impact_release_region ON impact(release_id, region);
CREATE INDEX idx_action_release_region ON readiness_action(release_id, region);
CREATE INDEX idx_defect_release_region ON defect(release_id, region);

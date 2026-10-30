-- ============================================================
-- V3 — Fix severity constraint: 1-5 → 1-4
-- Aligns DB with 4-button mobile UI (Low/Medium/High/Critical)
-- and sla_rule.severity_tier (4 values).
-- ============================================================

-- Handle any existing severity=5 rows before altering constraint
UPDATE complaint SET severity = 4 WHERE severity = 5;

ALTER TABLE complaint DROP CONSTRAINT IF EXISTS complaint_severity_check;
ALTER TABLE complaint ADD CONSTRAINT complaint_severity_check
    CHECK (severity BETWEEN 1 AND 4);

-- ============================================================
-- FIXLOOP — V5 Smart Auto-Clustering Enhancements
-- ============================================================

-- Mark complaints manually excluded from auto-clustering by admin
ALTER TABLE complaint
    ADD COLUMN IF NOT EXISTS cluster_excluded BOOLEAN NOT NULL DEFAULT FALSE;

-- Store computed cluster score at time of clustering (audit/debug)
ALTER TABLE complaint
    ADD COLUMN IF NOT EXISTS cluster_score NUMERIC(5,3);

-- Incident display summary = first complaint's description (set on incident creation, never overwritten)
ALTER TABLE incident
    ADD COLUMN IF NOT EXISTS summary TEXT;

-- Performance index: open incidents by category (used in findCandidates)
CREATE INDEX IF NOT EXISTS idx_incident_open_category
    ON incident(category_id, status)
    WHERE status = 'OPEN';

-- Sparse index: only for excluded complaints (very few rows)
CREATE INDEX IF NOT EXISTS idx_complaint_cluster_excluded
    ON complaint(cluster_excluded)
    WHERE cluster_excluded = TRUE;

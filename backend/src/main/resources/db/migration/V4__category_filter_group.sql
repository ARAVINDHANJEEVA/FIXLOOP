-- ============================================================
-- V4 — Add filter_group column to category
-- Drives mobile Home screen filter pills
-- Values: 'FACILITIES' | 'ACADEMIC_TECH' | 'WELFARE_MISSING' | NULL (sensitive)
-- ============================================================

ALTER TABLE category ADD COLUMN IF NOT EXISTS filter_group VARCHAR(30);

-- FixLoop Initial Seed Data
-- ============================================================
-- DEPARTMENTS
-- ============================================================
INSERT INTO departments (id, name, code, description, head_of_department)
VALUES 
  ('dept-1', 'Maintenance', 'MAINT', 'Handles hostel and building upkeep, plumbing, electrical, and carpentry', 'Rajesh Kumar'),
  ('dept-2', 'Catering & Food', 'FOOD', 'Manages food quality, cafeteria hygiene, and mess operations', 'Meena Sundaram'),
  ('dept-3', 'IT & Systems', 'IT', 'Handles all campus technology, network infrastructure, and labs', 'Arjun Nair'),
  ('dept-4', 'Student Affairs', 'STUD_AFF', 'Handles academic grievances, student welfare, health, and sensitive matters', 'Dr. Priya Sharma')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- CATEGORIES
-- ============================================================
INSERT INTO categories (id, name, icon, color, sensitive, base_severity_weight, default_sla_hours)
VALUES 
  ('cat-1', 'Hostel', 'Home', '#6366f1', false, 3, 24),
  ('cat-2', 'Food', 'Utensils', '#f59e0b', false, 2, 12),
  ('cat-3', 'Academic', 'BookOpen', '#3b82f6', false, 2, 48),
  ('cat-4', 'Health', 'Activity', '#ef4444', false, 4, 6),
  ('cat-5', 'Hygiene', 'Sparkles', '#22c55e', false, 2, 8),
  ('cat-6', 'Tech Support', 'Laptop', '#06b6d4', false, 2, 12),
  ('cat-7', 'Harassment', 'Lock', '#ec4899', true, 5, 24),
  ('cat-8', 'Malpractice', 'AlertTriangle', '#f97316', true, 5, 48),
  ('cat-9', 'Ragging', 'ShieldAlert', '#dc2626', true, 5, 12)
ON CONFLICT (id) DO NOTHING;

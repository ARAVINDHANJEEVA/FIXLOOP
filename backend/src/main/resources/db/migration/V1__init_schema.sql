-- ============================================================
-- FIXLOOP — V1 Full Schema Migration
-- PostgreSQL · 3NF/BCNF Normalized
-- ============================================================

-- ============================================================
-- REFERENCE / LOOKUP TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS department (
    id              SERIAL       PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    description     TEXT,
    head_of_dept    VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS skill (
    id              SERIAL       PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE
    -- Normalized entity. No more skill_tags text[] arrays anywhere.
    -- Typo-proof, FK-safe, fully indexable.
);

CREATE TABLE IF NOT EXISTS category (
    id                      SERIAL       PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL UNIQUE,
    icon                    VARCHAR(50),
    color                   VARCHAR(20),
    sensitive               BOOLEAN      NOT NULL DEFAULT FALSE,
    base_severity_weight    NUMERIC(4,2) NOT NULL DEFAULT 1.00,
    default_sla_hours       INTEGER      NOT NULL DEFAULT 24,
    department_id           INTEGER      REFERENCES department(id),
    form_schema             JSONB        NOT NULL DEFAULT '[]'::jsonb
    -- Legitimate JSONB use: schema truly varies per category (dynamic form fields).
    -- NOT used for structured relational data.
);

-- Junction: category ↔ required skills (replaces category.required_skill_tags text[])
CREATE TABLE IF NOT EXISTS category_required_skill (
    category_id     INTEGER NOT NULL REFERENCES category(id)  ON DELETE CASCADE,
    skill_id        INTEGER NOT NULL REFERENCES skill(id)     ON DELETE RESTRICT,
    PRIMARY KEY (category_id, skill_id)
    -- 2NF: no non-key attributes; 3NF: no transitive deps
);

CREATE TABLE IF NOT EXISTS location (
    id              SERIAL       PRIMARY KEY,
    building        VARCHAR(100) NOT NULL,
    floor           VARCHAR(30),
    room            VARCHAR(30),
    campus_zone     VARCHAR(50),
    UNIQUE (building, floor, room)
);

-- ============================================================
-- USERS — Unified auth table (avoids duplicating login logic)
-- ============================================================

CREATE TABLE IF NOT EXISTS app_user (
    id                  SERIAL       PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','ADMIN','STAFF')),
    restricted_access   BOOLEAN      NOT NULL DEFAULT FALSE,
    avatar_initials     VARCHAR(5),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_restricted_access_admin_only CHECK (restricted_access = FALSE OR role = 'ADMIN')
);

CREATE TABLE IF NOT EXISTS student_profile (
    user_id                 INTEGER PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    roll_no                 VARCHAR(30) NOT NULL UNIQUE,
    department_id           INTEGER     REFERENCES department(id),
    default_location_id     INTEGER     REFERENCES location(id)
);

CREATE TABLE IF NOT EXISTS admin_profile (
    user_id             INTEGER  PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    authority_level     SMALLINT NOT NULL DEFAULT 1
    -- 1 = standard admin, 2 = tier-2 authority, 3 = chief proctor
);

CREATE TABLE IF NOT EXISTS staff_profile (
    user_id                 INTEGER      PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    department_id           INTEGER      NOT NULL REFERENCES department(id),
    job_title               VARCHAR(100),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','INACTIVE','ON_LEAVE')),
    current_weighted_load   NUMERIC(8,2) NOT NULL DEFAULT 0
    -- Controlled denormalization: SUM(priority_score) of all open assigned complaints.
    -- Updated atomically in the same transaction as assign/resolve.
    -- Violates strict 3NF but eliminates hot-path SUM() on auto-assignment query.
);

-- Junction: staff ↔ skills (replaces staff.skill_tags text[])
CREATE TABLE IF NOT EXISTS staff_skill (
    staff_user_id   INTEGER NOT NULL REFERENCES staff_profile(user_id) ON DELETE CASCADE,
    skill_id        INTEGER NOT NULL REFERENCES skill(id)              ON DELETE RESTRICT,
    PRIMARY KEY (staff_user_id, skill_id)
);

-- ============================================================
-- VENDORS
-- ============================================================

CREATE TABLE IF NOT EXISTS vendor (
    id              SERIAL       PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(150),
    contact_person  VARCHAR(100)
);

-- Junction: vendor ↔ skills (replaces vendor.category_tags text[])
CREATE TABLE IF NOT EXISTS vendor_skill (
    vendor_id       INTEGER NOT NULL REFERENCES vendor(id) ON DELETE CASCADE,
    skill_id        INTEGER NOT NULL REFERENCES skill(id)  ON DELETE RESTRICT,
    PRIMARY KEY (vendor_id, skill_id)
);

-- ============================================================
-- SLA CONFIGURATION
-- ============================================================

CREATE TABLE IF NOT EXISTS sla_rule (
    id                  SERIAL       PRIMARY KEY,
    category_id         INTEGER      NOT NULL REFERENCES category(id),
    severity_tier       VARCHAR(10)  NOT NULL CHECK (severity_tier IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    resolution_hours    INTEGER      NOT NULL,
    escalation_hours    INTEGER      NOT NULL,
    UNIQUE (category_id, severity_tier)
    -- BCNF: (category_id, severity_tier) is the sole candidate key.
    -- resolution_hours and escalation_hours depend only on this composite key. No violation.
);

CREATE TABLE IF NOT EXISTS priority_weight_config (
    id              SERIAL       PRIMARY KEY,
    w1_severity     NUMERIC(5,3) NOT NULL DEFAULT 0.480,
    w2_students     NUMERIC(5,3) NOT NULL DEFAULT 0.210,
    w3_recurrence   NUMERIC(5,3) NOT NULL DEFAULT 0.080,
    w4_time_decay   NUMERIC(5,3) NOT NULL DEFAULT 0.230,
    effective_from  TIMESTAMP    NOT NULL DEFAULT NOW()
    -- Versioned config: history of weight changes preserved.
    -- Backend always uses MAX(effective_from) row — tunable post-demo.
);

-- ============================================================
-- INCIDENTS (duplicate/cluster grouping)
-- ============================================================

CREATE TABLE IF NOT EXISTS incident (
    id                  SERIAL       PRIMARY KEY,
    category_id         INTEGER      NOT NULL REFERENCES category(id),
    location_id         INTEGER      NOT NULL REFERENCES location(id),
    cluster_key         VARCHAR(200) NOT NULL,   -- "{category_id}:{location_id}:{YYYY-MM-DD}"
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN','RESOLVED','ARCHIVED')),
    complaint_count     INTEGER      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_complaint_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- COMPLAINTS (core entity)
-- ============================================================

CREATE TABLE IF NOT EXISTS complaint (
    id                      SERIAL       PRIMARY KEY,
    token                   VARCHAR(30)  NOT NULL UNIQUE,
    student_user_id         INTEGER      REFERENCES student_profile(user_id),  -- NULL for anonymous
    is_anonymous            BOOLEAN      NOT NULL DEFAULT FALSE,
    category_id             INTEGER      NOT NULL REFERENCES category(id),
    location_id             INTEGER      NOT NULL REFERENCES location(id),
    incident_id             INTEGER      REFERENCES incident(id),
    description             TEXT         NOT NULL,
    severity                SMALLINT     NOT NULL CHECK (severity BETWEEN 1 AND 5),
    students_affected       INTEGER      NOT NULL DEFAULT 1,
    recurrence_count_90d    INTEGER      NOT NULL DEFAULT 0,
    status                  VARCHAR(40)  NOT NULL DEFAULT 'SUBMITTED'
                            CHECK (status IN (
                                'SUBMITTED','UNDER_ADMIN_REVIEW','ASSIGNED',
                                'IN_PROGRESS','AWAITING_STUDENT_INFO',
                                'VENDOR_ASSIGNED','RESOLVED','REOPENED','CLOSED'
                            )),
    priority_score          NUMERIC(6,2) NOT NULL DEFAULT 0,
    extra_data              JSONB,
    -- Legitimate JSONB use: category-specific dynamic form responses.
    -- Schema is captured in category.form_schema; values stored here.
    assigned_by_admin_id    INTEGER      REFERENCES admin_profile(user_id),
    assigned_to_staff_id    INTEGER      REFERENCES staff_profile(user_id),
    routed_department_id    INTEGER      REFERENCES department(id),
    dispatch_type           VARCHAR(20)  CHECK (dispatch_type IN ('INTERNAL_STAFF','EXTERNAL_VENDOR')),
    escalation_level        SMALLINT     NOT NULL DEFAULT 0,
    is_at_risk              BOOLEAN      NOT NULL DEFAULT FALSE,
    is_breached             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    sla_deadline            TIMESTAMP,
    resolved_at             TIMESTAMP,

    CONSTRAINT chk_anon_no_student CHECK (NOT is_anonymous OR student_user_id IS NULL)
);

CREATE TABLE IF NOT EXISTS complaint_vendor (
    complaint_id    INTEGER      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    vendor_id       INTEGER      NOT NULL REFERENCES vendor(id),
    assigned_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    contact_status  VARCHAR(20)  NOT NULL DEFAULT 'CONTACTED'
                    CHECK (contact_status IN ('CONTACTED','CONFIRMED','ON_SITE','DONE')),
    notes           TEXT,
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (complaint_id, vendor_id)
);

CREATE TABLE IF NOT EXISTS attachment (
    id              SERIAL       PRIMARY KEY,
    complaint_id    INTEGER      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    mime_type       VARCHAR(100),
    file_size_bytes BIGINT,
    uploaded_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS status_history (
    id              SERIAL       PRIMARY KEY,
    complaint_id    INTEGER      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    status          VARCHAR(40)  NOT NULL,
    changed_by_id   INTEGER,                     -- NOT FK: user may be deleted, history must remain
    changed_by_name VARCHAR(150),                -- denormed for permanent display
    changed_by_role VARCHAR(20)  CHECK (changed_by_role IN ('STUDENT','STAFF','ADMIN','SYSTEM')),
    note            TEXT,
    timestamp       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS escalation_log (
    id              SERIAL       PRIMARY KEY,
    complaint_id    INTEGER      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    from_staff_id   INTEGER      REFERENCES staff_profile(user_id),
    to_staff_id     INTEGER      REFERENCES staff_profile(user_id),
    previous_level  SMALLINT,
    new_level       SMALLINT,
    reason          TEXT,
    triggered_by    VARCHAR(10)  CHECK (triggered_by IN ('AUTO','MANUAL')),
    timestamp       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS review (
    id              SERIAL       PRIMARY KEY,
    complaint_id    INTEGER      NOT NULL UNIQUE REFERENCES complaint(id) ON DELETE CASCADE,
    rating          SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PERFORMANCE INDEXES
-- Mapped directly to query patterns 5.1 – 5.4 from projectDetails.md
-- ============================================================

-- 5.4 Auto-assignment: filter by dept + skill + status, ORDER BY load ASC
CREATE INDEX IF NOT EXISTS idx_staff_dept_status
    ON staff_profile(department_id, status);

CREATE INDEX IF NOT EXISTS idx_staff_skill_lookup
    ON staff_skill(skill_id, staff_user_id);

-- 5.3 SLA scheduler: partial index — only open complaints (stays small as closed data grows)
CREATE INDEX IF NOT EXISTS idx_complaint_sla_scan
    ON complaint(status, sla_deadline)
    WHERE status NOT IN ('RESOLVED','CLOSED');

-- Admin triage queue: priority sort, partial index
CREATE INDEX IF NOT EXISTS idx_complaint_priority
    ON complaint(priority_score DESC)
    WHERE status NOT IN ('RESOLVED','CLOSED');

-- 5.2 Incident clustering
CREATE INDEX IF NOT EXISTS idx_incident_cluster
    ON incident(category_id, location_id, status);

-- Staff "My Queue"
CREATE INDEX IF NOT EXISTS idx_complaint_staff
    ON complaint(assigned_to_staff_id, status);

-- Student "My Complaints"
CREATE INDEX IF NOT EXISTS idx_complaint_student
    ON complaint(student_user_id, status);

-- Status history timeline (ordered per complaint)
CREATE INDEX IF NOT EXISTS idx_status_history_cmp
    ON status_history(complaint_id, timestamp);

-- complaint.token is covered by the UNIQUE constraint (auto-indexed by PostgreSQL)

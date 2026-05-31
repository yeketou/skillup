-- =============================================================================
-- V3__class_management.sql
-- Subjects, class templates (recurring schedules), sessions, and enrollments
-- =============================================================================

-- ── Subjects ──────────────────────────────────────────────────────────────────
-- A subject is the topic being taught (Math, BM, Science, etc.).
-- Subjects are centre-wide, not branch-specific.
CREATE TABLE subjects (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(20)  NOT NULL,          -- e.g. "MATH", "BM", "SCI"
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_subject_code UNIQUE (code)
);

COMMENT ON TABLE  subjects          IS 'Subjects / topics taught at the centre (e.g. Math, BM, Science)';
COMMENT ON COLUMN subjects.code     IS 'Short uppercase code shown in timetables, e.g. MATH, BM, SCI';
COMMENT ON COLUMN subjects.is_active IS 'FALSE = archived subject, no longer offered';

-- ── Class Templates (recurring schedule definition) ───────────────────────────
-- A class template defines the recurring timetable entry:
-- "Math Year 4 — every Monday, 10:00-11:30, at HQ branch, max 15 students"
CREATE TABLE class_templates (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,          -- e.g. "Math Year 4 Morning"
    subject_id      UUID         NOT NULL REFERENCES subjects(id),
    branch_id       UUID         NOT NULL REFERENCES branches(id),
    teacher_name    VARCHAR(150),                   -- free-text; no teacher entity yet in Phase 2
    day_of_week     VARCHAR(9)   NOT NULL,          -- MONDAY..SUNDAY
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    max_capacity    INT          NOT NULL DEFAULT 20,
    fee_amount      NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date      DATE         NOT NULL,          -- first day this class can run
    end_date        DATE,                           -- last day; NULL = indefinite

    -- Audit
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_template_day      CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    CONSTRAINT chk_template_time     CHECK (end_time > start_time),
    CONSTRAINT chk_template_capacity CHECK (max_capacity > 0),
    CONSTRAINT chk_template_fee      CHECK (fee_amount >= 0)
);

COMMENT ON TABLE  class_templates              IS 'Recurring weekly class definitions (timetable slots)';
COMMENT ON COLUMN class_templates.day_of_week  IS 'Day of week this class recurs: MONDAY..SUNDAY';
COMMENT ON COLUMN class_templates.fee_amount   IS 'Monthly fee charged per student for this class';

-- ── Class Sessions (individual class occurrences) ─────────────────────────────
-- Generated from a template: one row per actual class date.
-- Sessions are created in bulk for a term/month via the generate API.
CREATE TABLE class_sessions (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    template_id     UUID         NOT NULL REFERENCES class_templates(id),
    session_date    DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    teacher_name    VARCHAR(150),                   -- can differ from template (substitute)
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
                                                    -- SCHEDULED | COMPLETED | CANCELLED
    notes           TEXT,

    -- Audit
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_session_status  CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED')),
    CONSTRAINT uq_session          UNIQUE (template_id, session_date)
);

COMMENT ON TABLE  class_sessions             IS 'Individual class occurrences generated from a template';
COMMENT ON COLUMN class_sessions.status      IS 'SCHEDULED = upcoming; COMPLETED = class ran; CANCELLED = class was cancelled';

-- ── Class Enrollments (student ↔ class template) ──────────────────────────────
-- Tracks which students are enrolled in which recurring class.
CREATE TABLE class_enrollments (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID         NOT NULL REFERENCES students(id),
    template_id     UUID         NOT NULL REFERENCES class_templates(id),
    enrolled_date   DATE         NOT NULL DEFAULT CURRENT_DATE,
    dropped_date    DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                                                    -- ACTIVE | DROPPED | COMPLETED
    notes           TEXT,

    -- Audit
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_enrollment           UNIQUE (student_id, template_id),
    CONSTRAINT chk_enrollment_status   CHECK (status IN ('ACTIVE','DROPPED','COMPLETED'))
);

COMMENT ON TABLE  class_enrollments          IS 'Students enrolled in a recurring class';
COMMENT ON COLUMN class_enrollments.status   IS 'ACTIVE = currently attending; DROPPED = withdrew; COMPLETED = term ended';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_subjects_active         ON subjects (is_active)          WHERE deleted_at IS NULL;
CREATE INDEX idx_templates_branch        ON class_templates (branch_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_templates_subject       ON class_templates (subject_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_templates_day           ON class_templates (day_of_week) WHERE deleted_at IS NULL;
CREATE INDEX idx_sessions_template_date  ON class_sessions (template_id, session_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_sessions_date           ON class_sessions (session_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_enrollments_student     ON class_enrollments (student_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_enrollments_template    ON class_enrollments (template_id) WHERE deleted_at IS NULL;

-- ── Seed data ─────────────────────────────────────────────────────────────────
INSERT INTO subjects (name, code, description) VALUES
    ('Mathematics',        'MATH',    'Arithmetic, algebra, geometry and problem solving'),
    ('Bahasa Malaysia',    'BM',      'Malay language — reading, writing, comprehension'),
    ('English Language',   'ENG',     'English grammar, comprehension and writing'),
    ('Science',            'SCI',     'Primary and secondary school science'),
    ('Chinese Language',   'CHIN',    'Mandarin Chinese for primary and secondary'),
    ('Additional Maths',   'ADD_MATH','SPM Additional Mathematics'),
    ('Physics',            'PHY',     'SPM Physics'),
    ('Chemistry',          'CHEM',    'SPM Chemistry'),
    ('Biology',            'BIO',     'SPM Biology'),
    ('Sejarah',            'SEJ',     'Malaysian History for UPSR / PT3 / SPM');

-- =============================================================================
-- V2__students.sql
-- Students, parent/guardian details, and portal accounts
-- =============================================================================

-- ── Student ID sequence ───────────────────────────────────────────────────────
-- Generates the numeric part of STU-YYYY-NNNN
CREATE SEQUENCE student_id_seq START 1 INCREMENT 1;

-- ── Students ──────────────────────────────────────────────────────────────────
CREATE TABLE students (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Auto-generated human-readable ID, e.g. STU-2026-0001
    student_ref         VARCHAR(20)  NOT NULL,

    -- Personal details
    full_name           VARCHAR(150) NOT NULL,
    preferred_name      VARCHAR(80),              -- nickname used in class
    ic_number           VARCHAR(20),              -- MyKad / passport number
    date_of_birth       DATE,
    gender              VARCHAR(10)  NOT NULL DEFAULT 'UNSPECIFIED',
                                                   -- MALE | FEMALE | UNSPECIFIED
    nationality         VARCHAR(60)  DEFAULT 'Malaysian',
    photo_url           VARCHAR(500),              -- S3 key

    -- Academic details
    school_name         VARCHAR(150),
    current_grade       VARCHAR(30),               -- e.g. "Form 3", "Standard 5", "Year 10"

    -- Enrollment status
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                                                   -- ACTIVE | INACTIVE | SUSPENDED | GRADUATED
    branch_id           UUID         NOT NULL REFERENCES branches(id),
    enrolled_date       DATE         NOT NULL DEFAULT CURRENT_DATE,
    graduated_date      DATE,
    notes               TEXT,                      -- internal admin notes

    -- Audit
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted_at          TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_student_ref    UNIQUE (student_ref),
    CONSTRAINT chk_student_gender CHECK (gender IN ('MALE','FEMALE','UNSPECIFIED')),
    CONSTRAINT chk_student_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED','GRADUATED'))
);

COMMENT ON TABLE  students              IS 'One row per student enrolled at the tuition centre';
COMMENT ON COLUMN students.student_ref  IS 'Human-readable ID: STU-YYYY-NNNN, shown on receipts and reports';
COMMENT ON COLUMN students.ic_number    IS 'MyKad (12 digits) or passport number — stored but never indexed in plain text';
COMMENT ON COLUMN students.status       IS 'ACTIVE = attending; GRADUATED = completed; SUSPENDED = fees overdue';

-- ── Parent / Guardians ────────────────────────────────────────────────────────
-- Each student has one primary parent contact (used for billing & WhatsApp)
-- and optionally a second emergency contact.
CREATE TABLE student_parents (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id          UUID         NOT NULL REFERENCES students(id),

    -- Contact type
    relationship        VARCHAR(30)  NOT NULL DEFAULT 'PARENT',
                                                   -- PARENT | GUARDIAN | SIBLING | OTHER
    is_primary          BOOLEAN      NOT NULL DEFAULT TRUE,  -- primary = billing contact

    -- Identity
    full_name           VARCHAR(150) NOT NULL,
    ic_number           VARCHAR(20),
    phone               VARCHAR(20)  NOT NULL,
    email               VARCHAR(150),              -- primary parent email = portal login username
    whatsapp_number     VARCHAR(20),               -- defaults to phone if blank
    preferred_language  VARCHAR(10)  NOT NULL DEFAULT 'MS',
                                                   -- MS = Bahasa Malaysia | EN = English
    -- Audit
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted_at          TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_parent_relationship CHECK (relationship IN ('PARENT','GUARDIAN','SIBLING','OTHER')),
    CONSTRAINT chk_parent_language     CHECK (preferred_language IN ('MS','EN'))
);

COMMENT ON TABLE  student_parents            IS 'Parent/guardian contacts for each student';
COMMENT ON COLUMN student_parents.is_primary IS 'TRUE = billing & WhatsApp reminders go to this person';
COMMENT ON COLUMN student_parents.email      IS 'Primary parent email becomes the Student Portal login username';

-- ── Student Portal Accounts ───────────────────────────────────────────────────
-- One portal account per primary parent email.
-- Both parent AND student share the same Keycloak credentials.
-- A portal account can link to multiple students (siblings).
CREATE TABLE student_portal_accounts (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    keycloak_id     UUID,                          -- populated when Keycloak user is created
    email           VARCHAR(150) NOT NULL,          -- matches student_parents.email
    full_name       VARCHAR(150) NOT NULL,          -- parent name shown on portal login
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_portal_account_email     UNIQUE (email),
    CONSTRAINT uq_portal_account_keycloak  UNIQUE (keycloak_id)
);

-- Junction: which students belong to a portal account (handles siblings)
CREATE TABLE portal_account_students (
    portal_account_id   UUID NOT NULL REFERENCES student_portal_accounts(id),
    student_id          UUID NOT NULL REFERENCES students(id),
    PRIMARY KEY (portal_account_id, student_id)
);

COMMENT ON TABLE student_portal_accounts IS 'One account per parent email; parent + student(s) share same login';
COMMENT ON TABLE portal_account_students IS 'Maps portal accounts to students — one account can have multiple siblings';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_students_branch       ON students (branch_id)    WHERE deleted_at IS NULL;
CREATE INDEX idx_students_status       ON students (status)       WHERE deleted_at IS NULL;
CREATE INDEX idx_students_ref          ON students (student_ref);
CREATE INDEX idx_students_name         ON students (full_name)    WHERE deleted_at IS NULL;
CREATE INDEX idx_student_parents_stud  ON student_parents (student_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_student_parents_email ON student_parents (email)      WHERE deleted_at IS NULL;
CREATE INDEX idx_portal_account_email  ON student_portal_accounts (email) WHERE deleted_at IS NULL;

-- =============================================================================
-- V11__staff.sql
-- Staff accounts (teachers, admins), subject assignments, and teacher FK on
-- class templates.
-- =============================================================================

-- ── Staff reference sequence ──────────────────────────────────────────────────
CREATE SEQUENCE staff_id_seq START 1 INCREMENT 1;

-- ── Staff ─────────────────────────────────────────────────────────────────────
CREATE TABLE staff (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    staff_ref               VARCHAR(20)  NOT NULL,         -- STF-YYYY-NNNN
    full_name               VARCHAR(150) NOT NULL,
    ic_number               VARCHAR(20),
    phone                   VARCHAR(20),
    email                   VARCHAR(150),
    whatsapp_number         VARCHAR(20),
    role                    VARCHAR(20)  NOT NULL DEFAULT 'TEACHER',
                                                           -- TEACHER | CENTRE_ADMIN | BRANCH_ADMIN
    branch_id               UUID         REFERENCES branches(id),
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    notes                   TEXT,

    -- Keycloak portal account
    keycloak_id             UUID         UNIQUE,
    keycloak_provisioned    BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMPTZ,

    -- Audit
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(100),
    deleted_at              TIMESTAMPTZ,
    version                 BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_staff               PRIMARY KEY (id),
    CONSTRAINT uq_staff_ref           UNIQUE (staff_ref),
    CONSTRAINT chk_staff_role         CHECK (role IN ('TEACHER','CENTRE_ADMIN','BRANCH_ADMIN'))
);

COMMENT ON TABLE  staff                     IS 'Centre staff: teachers and admins';
COMMENT ON COLUMN staff.staff_ref           IS 'Human-readable reference, e.g. STF-2026-0001';
COMMENT ON COLUMN staff.role                IS 'TEACHER = class teacher; CENTRE_ADMIN = centre-wide admin; BRANCH_ADMIN = branch manager';
COMMENT ON COLUMN staff.keycloak_id         IS 'Keycloak user UUID (JWT sub claim) — set on portal activation';

-- ── Staff subjects (many-to-many) ─────────────────────────────────────────────
-- Tracks which subjects a staff member is qualified/assigned to teach.
CREATE TABLE staff_subjects (
    staff_id    UUID NOT NULL REFERENCES staff(id),
    subject_id  UUID NOT NULL REFERENCES subjects(id),
    CONSTRAINT pk_staff_subjects PRIMARY KEY (staff_id, subject_id)
);

CREATE INDEX idx_staff_subjects_staff   ON staff_subjects (staff_id);
CREATE INDEX idx_staff_subjects_subject ON staff_subjects (subject_id);

-- ── Link class templates to a staff member ────────────────────────────────────
-- Nullable: existing templates keep their free-text teacher_name until migrated.
ALTER TABLE class_templates
    ADD COLUMN teacher_id UUID REFERENCES staff(id);

CREATE INDEX idx_templates_teacher ON class_templates (teacher_id) WHERE deleted_at IS NULL;

-- ── Indexes on staff ──────────────────────────────────────────────────────────
CREATE INDEX idx_staff_branch    ON staff (branch_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_staff_role      ON staff (role)       WHERE deleted_at IS NULL;
CREATE INDEX idx_staff_active    ON staff (is_active)  WHERE deleted_at IS NULL;
CREATE INDEX idx_staff_keycloak  ON staff (keycloak_id);

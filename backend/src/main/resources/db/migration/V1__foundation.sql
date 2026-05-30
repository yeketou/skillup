-- =============================================================================
-- V1__foundation.sql
-- SkillUp foundation schema: branches (tuition centres) and system users
-- =============================================================================

-- ── Extensions ───────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ── Branches (Tuition Centres / Locations) ───────────────────────────────────
CREATE TABLE branches (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    code         VARCHAR(20)  NOT NULL,          -- short identifier e.g. "HQ", "PJ01"
    address      TEXT,
    phone        VARCHAR(20),
    email        VARCHAR(150),
    logo_url     VARCHAR(500),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Audit columns (managed by Spring Data JPA @CreationTimestamp / @UpdateTimestamp)
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMPTZ,                    -- NULL = active; soft-delete only
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_branch_code UNIQUE (code)
);

COMMENT ON TABLE  branches            IS 'Physical tuition centre locations';
COMMENT ON COLUMN branches.code       IS 'Short uppercase code used in display and API filtering';
COMMENT ON COLUMN branches.deleted_at IS 'NULL = active. Never hard-delete a branch with historical data.';

-- ── System Users (Admin Staff) ────────────────────────────────────────────────
-- Note: Students/Parents authenticate through the portal module (separate table).
-- This table mirrors Keycloak users for internal staff.
CREATE TABLE system_users (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    keycloak_id    UUID         NOT NULL,         -- Keycloak user UUID (sub claim)
    email          VARCHAR(150) NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    role           VARCHAR(50)  NOT NULL,          -- OWNER | ADMIN | MANAGER | CASHIER | TEACHER | ACCOUNTANT
    branch_id      UUID         REFERENCES branches(id),  -- NULL = platform-wide (OWNER/ADMIN)
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    deleted_at     TIMESTAMPTZ,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_system_user_keycloak UNIQUE (keycloak_id),
    CONSTRAINT uq_system_user_email    UNIQUE (email),
    CONSTRAINT chk_system_user_role    CHECK (role IN (
        'OWNER','ADMIN','MANAGER','CASHIER','TEACHER','ACCOUNTANT','VIEWER'
    ))
);

COMMENT ON TABLE  system_users            IS 'Internal staff accounts, mirrored from Keycloak';
COMMENT ON COLUMN system_users.branch_id  IS 'NULL for platform-wide roles (OWNER, ADMIN)';
COMMENT ON COLUMN system_users.role       IS 'Maps to Keycloak realm role; drives UI visibility';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_branches_is_active     ON branches    (is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_system_users_branch    ON system_users (branch_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_system_users_email     ON system_users (email)     WHERE deleted_at IS NULL;
CREATE INDEX idx_system_users_keycloak  ON system_users (keycloak_id);

-- ── Seed: default branch for first-time setup ─────────────────────────────────
INSERT INTO branches (id, name, code, is_active, created_by)
VALUES (
    gen_random_uuid(),
    'Main Branch',
    'HQ',
    TRUE,
    'system'
);

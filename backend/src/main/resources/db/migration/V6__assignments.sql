-- =============================================================================
-- V6__assignments.sql
-- Assignments, student submissions, and file attachments
-- =============================================================================

-- ── Assignments ───────────────────────────────────────────────────────────────
-- An assignment is given to an entire class (ClassTemplate), optionally tied
-- to a specific session. Publishing creates PENDING submission rows for every
-- currently enrolled student so teachers can track who has / hasn't submitted.
CREATE TABLE assignments (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    template_id     UUID          NOT NULL REFERENCES class_templates(id),

    -- Optional: assignment set during / after a specific class session
    session_id      UUID          REFERENCES class_sessions(id),

    title           VARCHAR(200)  NOT NULL,
    description     TEXT,

    -- HOMEWORK | PROJECT | QUIZ | CLASSWORK
    type            VARCHAR(20)   NOT NULL DEFAULT 'HOMEWORK',

    -- DRAFT (editable) → PUBLISHED (visible to students) → CLOSED (no more submissions)
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',

    -- Submission deadline (stored with timezone for precise cut-off)
    due_date        TIMESTAMPTZ   NOT NULL,

    -- Null = assignment is not graded (e.g. practice homework)
    max_score       NUMERIC(5,1),

    -- Allow submissions past the due date (marked as LATE instead of rejected)
    allow_late      BOOLEAN       NOT NULL DEFAULT FALSE,

    -- Audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_assignment_type   CHECK (type   IN ('HOMEWORK','PROJECT','QUIZ','CLASSWORK')),
    CONSTRAINT chk_assignment_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED')),
    CONSTRAINT chk_assignment_score  CHECK (max_score IS NULL OR max_score > 0)
);

COMMENT ON TABLE  assignments             IS 'Homework, quizzes and projects assigned to a class';
COMMENT ON COLUMN assignments.status      IS 'DRAFT = teacher editing; PUBLISHED = students can see/submit; CLOSED = no more submissions';
COMMENT ON COLUMN assignments.allow_late  IS 'TRUE = submissions past due_date accepted but marked LATE';

-- ── Assignment Submissions ────────────────────────────────────────────────────
-- One row per student per assignment.
-- Created automatically (status = PENDING) when an assignment is published.
CREATE TABLE assignment_submissions (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    assignment_id   UUID          NOT NULL REFERENCES assignments(id),
    student_id      UUID          NOT NULL REFERENCES students(id),

    -- PENDING → SUBMITTED (or LATE) → GRADED
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',

    -- Student's text answer / notes
    content         TEXT,

    -- When the student actually submitted (null until submitted)
    submitted_at    TIMESTAMPTZ,

    -- Grading
    score           NUMERIC(5,1),
    feedback        TEXT,
    graded_at       TIMESTAMPTZ,
    graded_by       VARCHAR(100),

    -- Audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uq_submission          UNIQUE (assignment_id, student_id),
    CONSTRAINT chk_submission_status  CHECK  (status IN ('PENDING','SUBMITTED','LATE','GRADED','EXCUSED'))
);

COMMENT ON TABLE  assignment_submissions        IS 'Student submission for an assignment';
COMMENT ON COLUMN assignment_submissions.status IS 'PENDING = not yet submitted; SUBMITTED = on time; LATE = after due date; GRADED = teacher has scored it; EXCUSED = exempted';

-- ── Submission Attachments ────────────────────────────────────────────────────
-- File attachments uploaded with a submission (PDF, JPG, PNG).
-- Only metadata stored here; files live in S3/MinIO (Phase 9 wires the upload).
CREATE TABLE submission_attachments (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    submission_id   UUID          NOT NULL REFERENCES assignment_submissions(id),

    file_key        VARCHAR(500)  NOT NULL,   -- S3 / MinIO object key
    file_name       VARCHAR(255)  NOT NULL,   -- original filename shown in UI
    file_size       BIGINT        NOT NULL,   -- bytes
    content_type    VARCHAR(100)  NOT NULL,   -- application/pdf | image/jpeg | image/png

    -- Audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_attachment_type CHECK (content_type IN ('application/pdf','image/jpeg','image/png'))
);

COMMENT ON TABLE submission_attachments IS 'File attachments for a student submission (PDF/image metadata; files in S3/MinIO)';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_assignments_template    ON assignments           (template_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_status      ON assignments           (status)        WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_due_date    ON assignments           (due_date)      WHERE deleted_at IS NULL;
CREATE INDEX idx_submissions_assignment  ON assignment_submissions (assignment_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_submissions_student     ON assignment_submissions (student_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_submissions_status      ON assignment_submissions (status)       WHERE deleted_at IS NULL;
CREATE INDEX idx_attachments_submission  ON submission_attachments (submission_id) WHERE deleted_at IS NULL;

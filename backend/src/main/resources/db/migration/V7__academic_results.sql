-- =============================================================================
-- V7__academic_results.sql
-- Student exam result tracking (school exams + internal tuition tests)
-- =============================================================================

-- ── Exam Results ──────────────────────────────────────────────────────────────
-- Records a student's score for any exam or test.
-- Covers both:
--   • School exams (brought in by the student — e.g. mid-year Maths paper)
--   • Internal tests set by the tuition centre (linked to a class template)
CREATE TABLE exam_results (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID          NOT NULL REFERENCES students(id),

    -- Optional links — null for external school exams not tied to our classes
    subject_id      UUID          REFERENCES subjects(id),
    template_id     UUID          REFERENCES class_templates(id),
    branch_id       UUID          REFERENCES branches(id),

    -- Exam identity
    exam_name       VARCHAR(200)  NOT NULL,   -- e.g. "Peperiksaan Pertengahan Tahun 2026"
    subject_label   VARCHAR(100),             -- free-text subject for school exams
                                              -- (ignored when subject_id is set)

    -- SCHOOL_EXAM | INTERNAL_EXAM | TRIAL_EXAM | MOCK_EXAM
    exam_type       VARCHAR(30)   NOT NULL DEFAULT 'INTERNAL_EXAM',

    exam_date       DATE          NOT NULL,

    -- Scoring
    score           NUMERIC(5,1)  NOT NULL,
    max_score       NUMERIC(5,1)  NOT NULL DEFAULT 100,

    -- Malaysian grading: A+, A, A-, B+, B, B-, C+, C, C-, D, E
    -- Auto-calculated by the app; may be overridden manually.
    grade           VARCHAR(5),

    notes           TEXT,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_exam_type     CHECK (exam_type IN ('SCHOOL_EXAM','INTERNAL_EXAM','TRIAL_EXAM','MOCK_EXAM')),
    CONSTRAINT chk_exam_score    CHECK (score >= 0 AND score <= max_score),
    CONSTRAINT chk_exam_max_pos  CHECK (max_score > 0)
);

COMMENT ON TABLE  exam_results              IS 'Student exam scores — school exams and internal tuition tests';
COMMENT ON COLUMN exam_results.subject_id   IS 'Link to subjects table — null for school exams outside our system';
COMMENT ON COLUMN exam_results.subject_label IS 'Free-text subject name for school exams (used when subject_id is null)';
COMMENT ON COLUMN exam_results.grade        IS 'Malaysian grading: A+/A/A-/B+/B/B-/C+/C/C-/D/E. Auto-calculated from score %.';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_exam_results_student    ON exam_results (student_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_exam_results_subject    ON exam_results (subject_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_exam_results_template   ON exam_results (template_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_exam_results_branch     ON exam_results (branch_id)    WHERE deleted_at IS NULL;
CREATE INDEX idx_exam_results_date       ON exam_results (exam_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_exam_results_type       ON exam_results (exam_type)    WHERE deleted_at IS NULL;

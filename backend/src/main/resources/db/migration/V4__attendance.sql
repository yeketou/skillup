-- =============================================================================
-- V4__attendance.sql
-- Attendance records: one row per student per class session
-- =============================================================================

CREATE TABLE attendance_records (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id  UUID        NOT NULL REFERENCES class_sessions(id),
    student_id  UUID        NOT NULL REFERENCES students(id),

    -- PRESENT | ABSENT | LATE | EXCUSED
    status      VARCHAR(10) NOT NULL DEFAULT 'ABSENT',

    -- Reason for absence, lateness, or excuse note
    notes       TEXT,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uq_attendance         UNIQUE (session_id, student_id),
    CONSTRAINT chk_attendance_status CHECK  (status IN ('PRESENT','ABSENT','LATE','EXCUSED'))
);

COMMENT ON TABLE  attendance_records        IS 'One attendance mark per student per class session';
COMMENT ON COLUMN attendance_records.status IS 'PRESENT = attended; ABSENT = did not attend; LATE = arrived late; EXCUSED = absent with valid reason';

-- Indexes
CREATE INDEX idx_attendance_session  ON attendance_records (session_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_attendance_student  ON attendance_records (student_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_attendance_status   ON attendance_records (status)      WHERE deleted_at IS NULL;

-- =============================================================================
-- V8__communications.sql
-- WhatsApp message templates and outbound communication logs
-- =============================================================================

-- ── Message Templates ─────────────────────────────────────────────────────────
-- Reusable message bodies with {{placeholder}} tokens.
-- Administrators manage these via the API; the system renders them at send time.
-- For Meta WhatsApp Business API, wa_template_name must match a pre-approved
-- template name in the Meta Business Manager.
CREATE TABLE message_templates (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Internal identifier used in code — e.g. "FEE_OVERDUE_REMINDER"
    name             VARCHAR(100) NOT NULL UNIQUE,

    -- FEE_REMINDER | ATTENDANCE_ALERT | ASSIGNMENT_REMINDER | WELCOME | GENERAL
    category         VARCHAR(50)  NOT NULL,

    description      TEXT,

    -- Message body with {{placeholder}} tokens, e.g. {{studentName}}, {{amount}}
    content          TEXT         NOT NULL,

    -- Meta WhatsApp Business pre-approved template name (required for prod
    -- business-initiated messages). Null = send as free-form text.
    wa_template_name VARCHAR(100),

    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_template_category CHECK (category IN
        ('FEE_REMINDER','ATTENDANCE_ALERT','ASSIGNMENT_REMINDER','WELCOME','GENERAL'))
);

COMMENT ON TABLE  message_templates                  IS 'WhatsApp message templates with {{placeholder}} tokens';
COMMENT ON COLUMN message_templates.wa_template_name IS 'Meta Business Manager pre-approved template name (required for business-initiated messages in prod)';

-- ── Communication Logs ────────────────────────────────────────────────────────
-- One row per outbound message attempt.
-- Captures what was sent, to whom, when, and whether it succeeded.
CREATE TABLE communication_logs (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Optional link to a student (null for ad-hoc admin messages)
    student_id          UUID         REFERENCES students(id),

    -- Optional link to the template used (null for ad-hoc plain-text messages)
    template_id         UUID         REFERENCES message_templates(id),

    recipient_phone     VARCHAR(30)  NOT NULL,   -- WhatsApp number, e.g. "60123456789"
    recipient_name      VARCHAR(200),

    -- Mirrors message_templates.category (stored denormalised for fast log filtering)
    message_category    VARCHAR(50)  NOT NULL,

    -- The fully rendered message that was (attempted to be) sent
    rendered_content    TEXT         NOT NULL,

    -- PENDING → SENT (or FAILED)
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    sent_at             TIMESTAMPTZ,
    provider_message_id VARCHAR(200),            -- "wamid.XXX" from Meta API response
    error_message       TEXT,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_log_status   CHECK (status IN ('PENDING','SENT','FAILED','DELIVERED')),
    CONSTRAINT chk_log_category CHECK (message_category IN
        ('FEE_REMINDER','ATTENDANCE_ALERT','ASSIGNMENT_REMINDER','WELCOME','GENERAL'))
);

COMMENT ON TABLE  communication_logs                    IS 'Audit trail of every outbound WhatsApp message';
COMMENT ON COLUMN communication_logs.provider_message_id IS 'wamid.XXX returned by Meta — used to match delivery webhooks';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_comm_logs_student   ON communication_logs (student_id)        WHERE deleted_at IS NULL;
CREATE INDEX idx_comm_logs_status    ON communication_logs (status)            WHERE deleted_at IS NULL;
CREATE INDEX idx_comm_logs_category  ON communication_logs (message_category)  WHERE deleted_at IS NULL;
CREATE INDEX idx_comm_logs_sent_at   ON communication_logs (sent_at DESC)      WHERE deleted_at IS NULL;

-- ── Seed: Default Message Templates ──────────────────────────────────────────
-- These can be edited via the API after deployment.
INSERT INTO message_templates (name, category, description, content, wa_template_name) VALUES

('FEE_REMINDER',
 'FEE_REMINDER',
 'Sent when a fee is approaching or has reached its due date',
 E'Salam {{parentName}},\n\nIni adalah peringatan mesra bahawa yuran pengajian bagi *{{studentName}}* untuk kelas *{{className}}* bulan {{billingMonth}} berjumlah *{{netAmount}}* perlu dijelaskan sebelum {{dueDate}}.\n\nBaki tertunggak: *{{balance}}*\n\nSila hubungi kami untuk sebarang pertanyaan.\nTerima kasih. 🙏\n- SkillUp Tuition Centre',
 'skillup_fee_reminder'),

('FEE_OVERDUE',
 'FEE_REMINDER',
 'Sent for fees that are overdue — escalating urgency',
 E'Salam {{parentName}},\n\n⚠️ Yuran pengajian *{{studentName}}* untuk kelas *{{className}}* ({{billingMonth}}) telah *TERTUNGGAK selama {{daysOverdue}} hari*.\n\nJumlah tertunggak: *{{balance}}*\nTarikh tamat bayaran: {{dueDate}}\n\nSila jelaskan pembayaran secepat mungkin untuk mengelakkan gangguan kelas.\n\nHubungi kami jika ada sebarang kesulitan.\n- SkillUp Tuition Centre',
 'skillup_fee_overdue'),

('ATTENDANCE_ABSENT',
 'ATTENDANCE_ALERT',
 'Sent to parent when a student is marked absent',
 E'Salam {{parentName}},\n\nKami ingin memaklumkan bahawa *{{studentName}}* telah ditanda *TIDAK HADIR* ke kelas *{{className}}* ({{subject}}) pada {{sessionDate}}.\n\nJika ini adalah kesilapan atau anda ingin membuat sebarang makluman, sila hubungi kami.\n\nTerima kasih.\n- SkillUp Tuition Centre',
 'skillup_attendance_absent'),

('WELCOME',
 'WELCOME',
 'Sent to parent when a new student is registered',
 E'Selamat datang ke SkillUp Tuition Centre! 🎉\n\nSalam {{parentName}},\n\nKami dengan sukacitanya mengalu-alukan *{{studentName}}* (ID: {{studentRef}}) sebagai pelajar baru kami di cawangan *{{branchName}}*.\n\nMaklumat pelajar telah berjaya didaftarkan. Kami komited untuk membantu {{studentName}} mencapai kecemerlangan akademik!\n\nSebarang pertanyaan, sila hubungi kami.\n- SkillUp Tuition Centre',
 'skillup_welcome'),

('ASSIGNMENT_DUE',
 'ASSIGNMENT_REMINDER',
 'Sent to remind students/parents of an upcoming assignment deadline',
 E'Salam {{parentName}},\n\n📚 Peringatan Tugasan!\n\n*{{studentName}}* mempunyai tugasan yang perlu diserahkan:\n\nTugasan: *{{assignmentTitle}}*\nSubjek: {{subjectName}}\nTarikh Akhir: *{{dueDate}}*\n\nSila pastikan tugasan diserahkan sebelum tarikh akhir.\n\nTerima kasih.\n- SkillUp Tuition Centre',
 'skillup_assignment_due');

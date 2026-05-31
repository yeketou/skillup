-- =============================================================================
-- V5__fees.sql
-- Fee records and payment transactions
-- =============================================================================

-- ── Fee Records ───────────────────────────────────────────────────────────────
-- One row per student per class per billing month.
-- Generated in bulk at the start of each month; individually updated as payments come in.
CREATE TABLE fee_records (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID          NOT NULL REFERENCES students(id),
    template_id     UUID          NOT NULL REFERENCES class_templates(id),

    -- Billing month stored as the first day of the month (e.g. 2026-06-01 = June 2026)
    billing_month   DATE          NOT NULL,

    -- Financials
    amount          NUMERIC(10,2) NOT NULL,                  -- gross fee from class template
    discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,     -- optional discount granted
    paid_amount     NUMERIC(10,2) NOT NULL DEFAULT 0.00,     -- running total of payments received

    due_date        DATE          NOT NULL,                  -- payment deadline
    status          VARCHAR(10)   NOT NULL DEFAULT 'PENDING',

    notes           TEXT,

    -- Audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0,

    -- One fee record per student-class-month
    CONSTRAINT uq_fee_record     UNIQUE (student_id, template_id, billing_month),
    CONSTRAINT chk_fee_status    CHECK  (status IN ('PENDING','PARTIAL','PAID','OVERDUE','WAIVED')),
    CONSTRAINT chk_fee_amount    CHECK  (amount >= 0),
    CONSTRAINT chk_fee_discount  CHECK  (discount_amount >= 0),
    CONSTRAINT chk_fee_paid      CHECK  (paid_amount >= 0)
);

COMMENT ON TABLE  fee_records              IS 'Monthly fee charge per student per class';
COMMENT ON COLUMN fee_records.billing_month IS 'First day of the billing month (e.g. 2026-06-01 = June 2026)';
COMMENT ON COLUMN fee_records.status       IS 'PENDING = unpaid within due date; PARTIAL = partly paid; PAID = fully paid; OVERDUE = unpaid past due date; WAIVED = written off';

-- ── Fee Payments ──────────────────────────────────────────────────────────────
-- Individual payment transactions against a fee record.
-- A single fee can have multiple payment rows (e.g. 2 instalments).
CREATE TABLE fee_payments (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    fee_record_id    UUID          NOT NULL REFERENCES fee_records(id),

    amount_paid      NUMERIC(10,2) NOT NULL,
    payment_date     DATE          NOT NULL DEFAULT CURRENT_DATE,
    payment_method   VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    reference_number VARCHAR(100),                           -- bank ref, receipt no., etc.
    notes            TEXT,

    -- Audit
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    deleted_at       TIMESTAMPTZ,
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_payment_method  CHECK (payment_method IN ('CASH','BANK_TRANSFER','ONLINE','CARD')),
    CONSTRAINT chk_payment_amount  CHECK (amount_paid > 0)
);

COMMENT ON TABLE fee_payments IS 'Payment transactions against a fee record (supports partial/instalment payments)';

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_fee_records_student        ON fee_records (student_id)               WHERE deleted_at IS NULL;
CREATE INDEX idx_fee_records_template       ON fee_records (template_id)              WHERE deleted_at IS NULL;
CREATE INDEX idx_fee_records_billing_month  ON fee_records (billing_month)            WHERE deleted_at IS NULL;
CREATE INDEX idx_fee_records_status         ON fee_records (status)                   WHERE deleted_at IS NULL;
CREATE INDEX idx_fee_records_due_date       ON fee_records (due_date)                 WHERE deleted_at IS NULL;
CREATE INDEX idx_fee_payments_fee_record    ON fee_payments (fee_record_id)           WHERE deleted_at IS NULL;

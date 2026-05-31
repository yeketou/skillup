-- Phase 11: public holidays table
-- Dates registered here are skipped by the session auto-generation scheduler.
-- Admin staff manage this list via POST/DELETE /admin/public-holidays.

CREATE TABLE public_holidays (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    holiday_date  DATE         NOT NULL,
    name          VARCHAR(150) NOT NULL,
    year          INT          NOT NULL,

    -- BaseEntity audit columns
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100),
    deleted_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_public_holidays      PRIMARY KEY (id),
    CONSTRAINT uq_public_holidays_date UNIQUE (holiday_date)
);

CREATE INDEX idx_public_holidays_year ON public_holidays (year);

-- Widen students.gender from varchar(10) to varchar(15).
-- 'UNSPECIFIED' is 11 characters and was silently truncated / rejected at insert.
ALTER TABLE students ALTER COLUMN gender TYPE VARCHAR(15);

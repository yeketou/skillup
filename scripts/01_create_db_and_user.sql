-- =============================================================================
-- SkillUp — Local PostgreSQL Setup Script
--
-- Run this ONCE against your local PostgreSQL 16 instance.
--
-- How to run:
--   Option 1 — psql command line (as postgres superuser):
--     psql -U postgres -f scripts/01_create_db_and_user.sql
--
--   Option 2 — pgAdmin 4:
--     Open Query Tool → paste contents → Execute (F5)
--     NOTE: pgAdmin does not support the \connect command.
--           Use Option 1 (psql) or run the two sections manually in pgAdmin:
--           Section A connected to "postgres" DB, Section B connected to "skillup_db".
--
-- What this does:
--   1. Creates the skillup_db database
--   2. Creates skillup_app  — application user (used by Spring Boot + Flyway)
--   3. Creates skillup_reader — read-only user  (pgAdmin dashboards, BI tools)
--   4. Grants correct schema-level privileges
--   5. Creates the keycloak schema (Keycloak stores its tables here)
--   6. Installs extensions: pgcrypto, pg_trgm, unaccent, pg_stat_statements
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- SECTION A — Run connected to the "postgres" maintenance database
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Create the application database
CREATE DATABASE skillup_db
    ENCODING    'UTF8'
    LC_COLLATE  'en_US.UTF-8'
    LC_CTYPE    'en_US.UTF-8'
    TEMPLATE    template0;

-- 2. Create application user (Spring Boot + Flyway)
--    This user owns all application tables and runs all migrations.
--    Change the password before using in staging or production.
CREATE USER skillup_app WITH PASSWORD 'skillup_app_pass';

-- 3. Create read-only reporting user (pgAdmin, BI, ad-hoc queries)
--    Cannot INSERT / UPDATE / DELETE — safe to give to staff for reporting.
CREATE USER skillup_reader WITH PASSWORD 'skillup_reader_pass';

-- 4. Grant database-level connect rights
GRANT CONNECT ON DATABASE skillup_db TO skillup_app;
GRANT CONNECT ON DATABASE skillup_db TO skillup_reader;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECTION B — Now switch into skillup_db
-- psql users: \connect command below does this automatically
-- pgAdmin users: open a new query tool tab connected to skillup_db
-- ─────────────────────────────────────────────────────────────────────────────
\connect skillup_db

-- 5. Extensions
--    Must be installed by a superuser; available to all users afterwards.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";           -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";            -- fast LIKE / similarity search
CREATE EXTENSION IF NOT EXISTS "unaccent";           -- accent-insensitive search
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements"; -- query performance monitoring

-- 6. Public schema ownership
--    skillup_app owns the schema so Flyway can CREATE / ALTER tables
--    without needing superuser rights at runtime.
ALTER SCHEMA public OWNER TO skillup_app;

-- Tighten public schema: only the owner (skillup_app) can create objects
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- 7. Application user — full DML + DDL on public schema
GRANT USAGE, CREATE ON SCHEMA public TO skillup_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES    IN SCHEMA public TO skillup_app;
GRANT USAGE, SELECT, UPDATE          ON ALL SEQUENCES IN SCHEMA public TO skillup_app;
GRANT EXECUTE                        ON ALL FUNCTIONS IN SCHEMA public TO skillup_app;

-- Automatically extend privileges to future tables created by Flyway
ALTER DEFAULT PRIVILEGES FOR ROLE skillup_app IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES    TO skillup_app;
ALTER DEFAULT PRIVILEGES FOR ROLE skillup_app IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE          ON SEQUENCES TO skillup_app;
ALTER DEFAULT PRIVILEGES FOR ROLE skillup_app IN SCHEMA public
    GRANT EXECUTE                        ON FUNCTIONS TO skillup_app;

-- 8. Read-only user — SELECT only on public schema
GRANT USAGE ON SCHEMA public TO skillup_reader;

GRANT SELECT ON ALL TABLES    IN SCHEMA public TO skillup_reader;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO skillup_reader;

-- Extend to future tables
ALTER DEFAULT PRIVILEGES FOR ROLE skillup_app IN SCHEMA public
    GRANT SELECT ON TABLES    TO skillup_reader;
ALTER DEFAULT PRIVILEGES FOR ROLE skillup_app IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO skillup_reader;

-- 9. Keycloak schema
--    Keycloak (running in Docker) connects as the postgres superuser.
--    It creates its own tables inside this schema automatically on first boot.
CREATE SCHEMA IF NOT EXISTS keycloak AUTHORIZATION postgres;
GRANT ALL ON SCHEMA keycloak TO postgres;

-- ─────────────────────────────────────────────────────────────────────────────
-- DONE
-- ─────────────────────────────────────────────────────────────────────────────
-- Connection details for application.yml / .env:
--   url      : jdbc:postgresql://localhost:5432/skillup_db
--   username : skillup_app
--   password : skillup_app_pass
--
-- Next steps:
--   1. Start Docker services:  docker compose up -d
--   2. Run the backend:        cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
--   3. Flyway will create all tables automatically on startup.
--   4. Open Swagger:           http://localhost:8080/api/swagger-ui.html
-- ─────────────────────────────────────────────────────────────────────────────

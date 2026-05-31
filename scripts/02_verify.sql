-- =============================================================================
-- SkillUp — Database Verification Script
-- Script  : 02_verify.sql
-- Run as  : postgres (superuser) or skillup_app
-- Connect : skillup_db
--
-- Run this after 01_create_db_and_user.sql to confirm the setup is correct.
-- Also useful after any Flyway migration to review the current schema state.
-- =============================================================================

-- ── 1. Connection info ────────────────────────────────────────────────────────
SELECT
    current_database()  AS "Database",
    current_user        AS "Connected As",
    inet_server_addr()  AS "Server",
    inet_server_port()  AS "Port";

-- ── 2. Users and their privileges ────────────────────────────────────────────
SELECT
    r.rolname                                                          AS "User",
    r.rolcanlogin                                                      AS "Can Login",
    r.rolconnlimit                                                     AS "Max Connections",
    has_database_privilege(r.rolname, 'skillup_db', 'CONNECT')        AS "DB Connect",
    has_schema_privilege(r.rolname, 'public',   'USAGE')              AS "public USAGE",
    has_schema_privilege(r.rolname, 'public',   'CREATE')             AS "public CREATE",
    CASE WHEN has_schema_privilege(r.rolname, 'keycloak', 'USAGE')
         THEN TRUE ELSE FALSE END                                      AS "keycloak USAGE"
FROM pg_roles r
WHERE r.rolname IN ('postgres', 'skillup_app', 'skillup_reader')
ORDER BY r.rolname;

-- ── 3. Installed extensions ───────────────────────────────────────────────────
SELECT
    extname     AS "Extension",
    extversion  AS "Version"
FROM pg_extension
ORDER BY extname;

-- ── 4. All schemas ────────────────────────────────────────────────────────────
SELECT
    nspname                           AS "Schema",
    pg_get_userbyid(nspowner)         AS "Owner"
FROM pg_namespace
WHERE nspname NOT LIKE 'pg_%'
  AND nspname <> 'information_schema'
ORDER BY nspname;

-- ── 5. Tables (after Flyway has run) ─────────────────────────────────────────
SELECT
    tablename                AS "Table",
    tableowner               AS "Owner",
    hasindexes               AS "Has Indexes",
    pg_size_pretty(
        pg_total_relation_size(schemaname||'.'||tablename)
    )                        AS "Size"
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- ── 6. Sequences (should include student_id_seq after V2 migration) ───────────
SELECT
    sequencename  AS "Sequence",
    start_value   AS "Start",
    last_value    AS "Last Value",
    increment_by  AS "Increment"
FROM pg_sequences
WHERE schemaname = 'public'
ORDER BY sequencename;

-- ── 7. Default privileges (confirm future tables are auto-granted) ────────────
SELECT
    pg_get_userbyid(d.defaclrole)  AS "Grantor",
    pg_get_userbyid(acl.grantee)   AS "Grantee",
    d.defaclobjtype                AS "Object Type",
    acl.privilege_type             AS "Privilege"
FROM pg_default_acl d,
     aclexplode(d.defaclacl) acl
WHERE d.defaclnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
ORDER BY "Grantee", "Object Type", "Privilege";

-- ── 8. Flyway migration history ───────────────────────────────────────────────
-- Only shows after the app has started at least once.
-- If the table does not exist yet, this will return a "not run yet" message.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
    ) THEN
        RAISE NOTICE 'Flyway history found. Run this query manually to see migrations:';
        RAISE NOTICE 'SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;';
    ELSE
        RAISE NOTICE '[NOT YET RUN] flyway_schema_history does not exist.';
        RAISE NOTICE 'Start the Spring Boot app once to apply all Flyway migrations.';
        RAISE NOTICE 'Command: cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local';
    END IF;
END $$;

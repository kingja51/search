-- ============================================================
-- grant_search_user.sql : 앱 계정(search_user) 권한 부여
-- 이미 생성된 postgres DB의 search 스키마에 대해, postgres(관리자)로 접속해 실행한다.
--   "C:\Program Files\PostgreSQL\18\bin\psql" -U postgres -d postgres -f db/grant_search_user.sql
-- (DBeaver라면 postgres 계정 접속 상태에서 아래 전체 실행)
-- ============================================================

GRANT USAGE, CREATE ON SCHEMA search TO search_user;   -- CREATE: Flyway 이력 테이블·마이그레이션용

-- ★ 기존 객체 소유권 이전 (필수)
-- COMMENT ON, ALTER TABLE 등 DDL 마이그레이션은 "소유자"만 실행 가능하다.
-- postgres 계정으로 만든 객체를 앱 계정(Flyway 실행 주체)으로 이전한다.
-- (identity 시퀀스는 테이블에 종속되어 테이블 소유권을 따라 자동 이전됨)
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT c.relname, c.relkind
             FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
             WHERE n.nspname = 'search' AND c.relkind IN ('r', 'v', 'm') LOOP
        IF r.relkind = 'v' THEN
            EXECUTE format('ALTER VIEW search.%I OWNER TO search_user', r.relname);
        ELSIF r.relkind = 'm' THEN
            EXECUTE format('ALTER MATERIALIZED VIEW search.%I OWNER TO search_user', r.relname);
        ELSE
            EXECUTE format('ALTER TABLE search.%I OWNER TO search_user', r.relname);
        END IF;
    END LOOP;
END $$;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA search TO search_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA search TO search_user;

-- 이후 postgres 계정이 새로 만드는 객체에도 자동 부여
ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO search_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT USAGE, SELECT ON SEQUENCES TO search_user;

-- 인기 검색어 MV 자동 갱신(REFRESH MATERIALIZED VIEW CONCURRENTLY) 권한 (PostgreSQL 17+)
GRANT MAINTAIN ON search.vw_search_popular_keyword TO search_user;

-- ============================================================
-- grant_search_user.sql : 앱 계정(search_user) 권한 부여
-- 이미 생성된 postgres DB의 search 스키마에 대해, postgres(관리자)로 접속해 실행한다.
--   "C:\Program Files\PostgreSQL\18\bin\psql" -U postgres -d postgres -f db/grant_search_user.sql
-- (DBeaver라면 postgres 계정 접속 상태에서 아래 전체 실행)
-- ============================================================

GRANT USAGE, CREATE ON SCHEMA search TO search_user;   -- CREATE: Flyway 이력 테이블·V5+ 마이그레이션용

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA search TO search_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA search TO search_user;

-- 이후 postgres 계정이 새로 만드는 객체에도 자동 부여
ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO search_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT USAGE, SELECT ON SEQUENCES TO search_user;

-- 인기 검색어 MV 자동 갱신(REFRESH MATERIALIZED VIEW CONCURRENTLY) 권한 (PostgreSQL 17+)
GRANT MAINTAIN ON search.vw_search_popular_keyword TO search_user;

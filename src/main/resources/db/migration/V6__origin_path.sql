-- ============================================================
-- V6__origin_path.sql : 검색 VIEW에 원본파일전체경로(origin_path) 추가
-- 파일 텍스트 추출 배치가 원본 파일을 다시 읽기 위한 경로 정보.
-- UNION(vw_search_source) 구조 유지를 위해 4개 VIEW 모두 9번째 컬럼으로 추가 (파일 외에는 NULL).
-- ※ CREATE OR REPLACE VIEW는 "끝에 컬럼 추가"만 허용되므로 마지막 컬럼으로 붙인다.
-- ============================================================

CREATE OR REPLACE VIEW vw_content_search AS
SELECT 'CONTENT'                        AS doc_type,
       c.id                             AS doc_id,
       c.title                          AS title,
       c.content                        AS body,
       '/content/' || c.id              AS link_url,
       c.category                       AS category,
       c.updated_at                     AS updated_at,
       md5(c.title || '|' || c.content || '|' || coalesce(c.category,'')) AS content_hash,
       NULL::varchar                    AS origin_path
FROM tn_content c
WHERE c.status = 'ACTIVE';

CREATE OR REPLACE VIEW vw_file_search AS
SELECT 'FILE'                           AS doc_type,
       f.id                             AS doc_id,
       f.file_name                      AS title,
       coalesce(f.extract_text, '')     AS body,
       '/file/' || f.id                 AS link_url,
       f.file_ext                       AS category,
       f.updated_at                     AS updated_at,
       md5(f.file_name || '|' || coalesce(f.extract_text,'')) AS content_hash,
       f.file_path                      AS origin_path
FROM tn_file f
WHERE f.status = 'ACTIVE';

CREATE OR REPLACE VIEW vw_bbs_search AS
SELECT 'BBS'                            AS doc_type,
       b.id                             AS doc_id,
       b.title                          AS title,
       b.content                        AS body,
       '/bbs/' || b.board_cd || '/' || b.id AS link_url,
       b.board_cd                       AS category,
       b.updated_at                     AS updated_at,
       md5(b.title || '|' || b.content || '|' || b.board_cd) AS content_hash,
       NULL::varchar                    AS origin_path
FROM tn_bbs b
WHERE b.status = 'ACTIVE';

CREATE OR REPLACE VIEW vw_menu_search AS
SELECT 'MENU'                           AS doc_type,
       m.id                             AS doc_id,
       m.menu_name                      AS title,
       coalesce(m.description, '')      AS body,
       m.menu_path                      AS link_url,
       coalesce(nullif(split_part(m.menu_path, '/', 2), ''), 'home') AS category,
       m.updated_at                     AS updated_at,
       md5(m.menu_name || '|' || coalesce(m.description,'') || '|' || m.menu_path) AS content_hash,
       NULL::varchar                    AS origin_path
FROM tn_menu m
WHERE m.use_yn = 'Y';

-- 통합 소스 VIEW 재정의 (새 컬럼 반영 — SELECT *는 생성 시점 컬럼으로 고정되므로 재생성 필요)
CREATE OR REPLACE VIEW vw_search_source AS
SELECT * FROM vw_content_search
UNION ALL SELECT * FROM vw_file_search
UNION ALL SELECT * FROM vw_bbs_search
UNION ALL SELECT * FROM vw_menu_search;

COMMENT ON COLUMN vw_file_search.origin_path   IS '원본파일전체경로 — 파일 텍스트 추출 배치가 원본을 다시 읽는 경로';
COMMENT ON COLUMN vw_search_source.origin_path IS '원본파일전체경로 (FILE 외 NULL)';

-- ============================================================
-- V3__search_view.sql : 검색 VIEW 4종 + 통합 색인 테이블 + 인기 검색어 MV (DESIGN.md 3.5)
-- ============================================================

-- ── 컨텐츠 검색 소스 ──
CREATE VIEW vw_content_search AS
SELECT 'CONTENT'                        AS doc_type,
       c.id                             AS doc_id,
       c.title                          AS title,
       c.content                        AS body,
       '/content/' || c.id              AS link_url,
       c.category                       AS category,
       c.updated_at                     AS updated_at,
       md5(c.title || '|' || c.content || '|' || coalesce(c.category,'')) AS content_hash
FROM tn_content c
WHERE c.status = 'ACTIVE';

-- ── 파일 검색 소스 (파일명 + 추출 본문) ──
CREATE VIEW vw_file_search AS
SELECT 'FILE'                           AS doc_type,
       f.id                             AS doc_id,
       f.file_name                      AS title,
       coalesce(f.extract_text, '')     AS body,
       '/file/' || f.id                 AS link_url,
       f.file_ext                       AS category,
       f.updated_at                     AS updated_at,
       md5(f.file_name || '|' || coalesce(f.extract_text,'')) AS content_hash
FROM tn_file f
WHERE f.status = 'ACTIVE';

-- ── 게시판 검색 소스 ──
CREATE VIEW vw_bbs_search AS
SELECT 'BBS'                            AS doc_type,
       b.id                             AS doc_id,
       b.title                          AS title,
       b.content                        AS body,
       '/bbs/' || b.board_cd || '/' || b.id AS link_url,
       b.board_cd                       AS category,
       b.updated_at                     AS updated_at,
       md5(b.title || '|' || b.content || '|' || b.board_cd) AS content_hash
FROM tn_bbs b
WHERE b.status = 'ACTIVE';

-- ── 메뉴 검색 소스 ──
CREATE VIEW vw_menu_search AS
SELECT 'MENU'                           AS doc_type,
       m.id                             AS doc_id,
       m.menu_name                      AS title,
       coalesce(m.description, '')      AS body,
       m.menu_path                      AS link_url,
       coalesce(nullif(split_part(m.menu_path, '/', 2), ''), 'home') AS category,  -- 경로 1단계
       m.updated_at                     AS updated_at,
       md5(m.menu_name || '|' || coalesce(m.description,'') || '|' || m.menu_path) AS content_hash
FROM tn_menu m
WHERE m.use_yn = 'Y';

-- ── 통합 소스 VIEW (색인 파이프라인이 읽는 단일 진입점) ──
CREATE VIEW vw_search_source AS
SELECT * FROM vw_content_search
UNION ALL SELECT * FROM vw_file_search
UNION ALL SELECT * FROM vw_bbs_search
UNION ALL SELECT * FROM vw_menu_search;

-- ── 통합 검색 색인 테이블 (Nori 분석 결과 저장) ──
CREATE TABLE tn_search_index (
    doc_type     VARCHAR(20)  NOT NULL,        -- CONTENT / FILE / BBS / MENU
    doc_id       BIGINT       NOT NULL,
    title        VARCHAR(500) NOT NULL,
    summary      VARCHAR(2000),                -- 결과 목록 출력용 본문 (body 앞 2000자, 하이라이트 대상)
    link_url     VARCHAR(500) NOT NULL,
    category     VARCHAR(100),
    tokens       TEXT NOT NULL,                -- Nori 분석 결과 (공백 구분)
    content_hash VARCHAR(32) NOT NULL,         -- 색인 당시 vw_*_search.content_hash
    source_updated_at TIMESTAMPTZ NOT NULL,    -- 원본 수정일 (최신순 정렬·기간 필터·등록일 표기 기준)
    search_vec   TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', tokens)) STORED,
    -- 공통 감사 컬럼 (동기화 배치가 기록: created_by/updated_by = 'system', ip = 서버 IP)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,  -- = 마지막 색인 시각
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50),
    PRIMARY KEY (doc_type, doc_id)
);
CREATE INDEX idx_search_vec     ON tn_search_index USING GIN (search_vec);
CREATE INDEX idx_search_trgm    ON tn_search_index USING GIN (title gin_trgm_ops);  -- 자동완성용
CREATE INDEX idx_search_type    ON tn_search_index (doc_type, category);            -- 탭·카테고리 필터용
CREATE INDEX idx_search_updated ON tn_search_index (source_updated_at DESC);        -- 최신순·기간 필터용

-- ── 인기 검색어 집계 MV (로그 기반, 10분 주기 자동 갱신) ──
CREATE MATERIALIZED VIEW vw_search_popular_keyword AS
SELECT keyword,
       count(*)         AS search_count,
       max(created_at)  AS last_searched_at
FROM log_search_keyword
WHERE created_at >= now() - INTERVAL '7 days'
  AND is_blocked = false
GROUP BY keyword
ORDER BY search_count DESC
LIMIT 100;
CREATE UNIQUE INDEX uq_vw_popular ON vw_search_popular_keyword (keyword);
-- 갱신: REFRESH MATERIALIZED VIEW CONCURRENTLY vw_search_popular_keyword;

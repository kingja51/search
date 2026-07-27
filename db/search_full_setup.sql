-- ============================================================
-- search_full_setup.sql : PostgreSQL 18 수동 설치 정리본
-- search 스키마 생성 + 테이블/VIEW/MV 생성 + 샘플 데이터 INSERT (전 테이블 10건)
--
-- ※ 애플리케이션 실행 시에는 Flyway가 V1~V4를 자동 적용한다 (spring.flyway.default-schema=search).
--    이 파일은 "앱 없이 DB만 구성"하거나 전체 스키마를 한눈에 검토할 때 쓰는 정리본이며,
--    Flyway로 관리할 DB에는 직접 실행하지 말 것 (flyway_schema_history 없이 테이블이 생겨 충돌).
--
-- 실행 예 (postgres 관리자 계정으로, 대상 DB에 접속해 실행):
--   "C:\Program Files\PostgreSQL\18\bin\psql" -U postgres -d postgres -f db/search_full_setup.sql
-- ============================================================

-- ─────────────────────────────────────────────
-- 0) 애플리케이션 계정 (이미 있으면 이 두 구문은 건너뛰기)
-- ─────────────────────────────────────────────
CREATE USER search_user
WITH LOGIN PASSWORD 'search_pw!@';

GRANT ALL PRIVILEGES
ON DATABASE postgres              -- 접속 대상 DB (다른 DB를 쓰면 이름 변경)
TO search_user;

-- ─────────────────────────────────────────────
-- 1) 확장 설치
--    ※ WITH SCHEMA extensions 를 쓰려면 그 스키마가 먼저 존재해야 한다
--      (없으면 SQL Error 3F000: "extensions" 스키마 없음)
--    ※ 이미 다른 스키마(public 등)에 설치된 확장은 IF NOT EXISTS 로 건너뛴다.
--      이동이 필요하면: ALTER EXTENSION pg_trgm SET SCHEMA extensions;
-- ─────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS extensions;

CREATE EXTENSION IF NOT EXISTS pgcrypto      WITH SCHEMA extensions;  -- 암호화, 해시, 랜덤 바이트, UUID
CREATE EXTENSION IF NOT EXISTS pg_trgm       WITH SCHEMA extensions;  -- 유사 문자열·자동완성 (★ 검색엔진 필수)
CREATE EXTENSION IF NOT EXISTS citext        WITH SCHEMA extensions;  -- 대소문자 무시 문자열 타입
CREATE EXTENSION IF NOT EXISTS unaccent      WITH SCHEMA extensions;  -- 악센트 제거 전처리
CREATE EXTENSION IF NOT EXISTS btree_gin     WITH SCHEMA extensions;  -- GIN 복합 인덱스 일반 자료형
CREATE EXTENSION IF NOT EXISTS btree_gist    WITH SCHEMA extensions;  -- GiST 복합 인덱스 일반 자료형
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch WITH SCHEMA extensions;  -- Soundex/Levenshtein 등

-- ─────────────────────────────────────────────
-- 2) search 스키마 + 검색 경로
--    extensions 를 경로에 포함해야 gin_trgm_ops·similarity() 참조 가능
-- ─────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS search;
SET search_path TO search, extensions, public;

GRANT USAGE, CREATE ON SCHEMA search TO search_user;   -- CREATE: Flyway V5+ 마이그레이션용
GRANT USAGE ON SCHEMA extensions TO search_user;


-- ════════════════════════════════════════════════════════════
-- 원본: src/main/resources/db/migration/V1__dictionary_log.sql
-- ════════════════════════════════════════════════════════════

-- ============================================================
-- V1__dictionary_log.sql : 사전 4종 + 검색 키워드 로그 (DESIGN.md 3.3)
-- 공통 감사 컬럼: created_at/ip/by + updated_at/ip/by (전 테이블)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- 자동완성/유사검색용

-- ─────────────────────────────────────────────
-- 단어사전 (Nori 사용자 사전: 신조어·고유명사·복합명사 분해)
-- ─────────────────────────────────────────────
CREATE TABLE tn_search_dic_word (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    word        VARCHAR(100) NOT NULL,          -- 예: '아이폰15'
    segments    VARCHAR(200),                   -- 복합명사 분해형. 예: '아이폰 15' (NULL이면 단일어)
    pos_tag     VARCHAR(20)  DEFAULT 'NNG',
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    memo        VARCHAR(300),
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50),
    CONSTRAINT uq_dic_word UNIQUE (word)
);

-- ─────────────────────────────────────────────
-- 동의어사전 (같은 group_id = 서로 동의어)
-- ─────────────────────────────────────────────
CREATE TABLE tn_search_dic_synonym (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id          BIGINT       NOT NULL,
    word              VARCHAR(100) NOT NULL,    -- 예: 그룹1 = {휴대폰, 핸드폰, 스마트폰}
    is_representative BOOLEAN      NOT NULL DEFAULT false,
    enabled           BOOLEAN      NOT NULL DEFAULT true,
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50),
    CONSTRAINT uq_dic_synonym UNIQUE (group_id, word)
);
CREATE INDEX idx_dic_synonym_word ON tn_search_dic_synonym (word) WHERE enabled;

-- ─────────────────────────────────────────────
-- 금지어사전
-- ─────────────────────────────────────────────
CREATE TABLE tn_search_dic_banned (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    word        VARCHAR(100) NOT NULL,
    block_type  VARCHAR(20)  NOT NULL DEFAULT 'BLOCK',  -- BLOCK(검색차단) / MASK(결과숨김)
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    memo        VARCHAR(300),
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50),
    CONSTRAINT uq_dic_banned UNIQUE (word)
);

-- ─────────────────────────────────────────────
-- 추천 검색어 (관리자 등록. 어드민 개발 전까지는 SQL로 관리)
-- ─────────────────────────────────────────────
CREATE TABLE tn_search_recommend_keyword (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    keyword       VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,   -- 노출 순서 (낮을수록 먼저)
    start_date    DATE,                              -- 노출 시작일 (NULL=상시)
    end_date      DATE,                              -- 노출 종료일 (NULL=상시)
    enabled       BOOLEAN      NOT NULL DEFAULT true,
    memo          VARCHAR(300),
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50),
    CONSTRAINT uq_recommend_keyword UNIQUE (keyword)
);
CREATE INDEX idx_recommend_order ON tn_search_recommend_keyword (display_order) WHERE enabled;

-- ─────────────────────────────────────────────
-- 검색 키워드 로그
--   검색 시각 = created_at, 검색자 IP = created_ip, 검색자 ID = created_by
--   (감사 컬럼이 로그 본연의 컬럼을 겸한다 — searched_at/client_ip 별도 컬럼 없음)
-- ─────────────────────────────────────────────
CREATE TABLE log_search_keyword (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    keyword         VARCHAR(300) NOT NULL,      -- 사용자가 입력한 원본
    analyzed_tokens VARCHAR(500),               -- 형태소 분석 결과 토큰
    expanded_query  VARCHAR(1000),              -- 동의어 확장 후 최종 tsquery
    doc_type        VARCHAR(20),                -- 검색한 탭 (NULL=전체)
    result_count    INT          NOT NULL DEFAULT 0,
    is_blocked      BOOLEAN      NOT NULL DEFAULT false,
    session_id      VARCHAR(64),                -- "내 검색어" 1차 식별 키
    trace_id        VARCHAR(32),                -- 앱 로그(traceId)와 상호 추적용
    elapsed_ms      INT,
    -- 공통 감사 컬럼 (created_at=검색 시각, created_ip=검색자 IP, created_by=검색자 ID/guest)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'guest',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50)
);
CREATE INDEX idx_lsk_keyword ON log_search_keyword (keyword);
CREATE INDEX idx_lsk_created ON log_search_keyword (created_at);
CREATE INDEX idx_lsk_ip      ON log_search_keyword (created_ip, created_at DESC);  -- "내 검색어" 조회용


-- ════════════════════════════════════════════════════════════
-- 원본: src/main/resources/db/migration/V2__sample_source.sql
-- ════════════════════════════════════════════════════════════

-- ============================================================
-- V2__sample_source.sql : 검색 대상 샘플 원본 4종 (DESIGN.md 3.4)
-- ============================================================

-- ── 컨텐츠 ──
CREATE TABLE tn_content (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(500)  NOT NULL,
    content     TEXT          NOT NULL,
    category    VARCHAR(100),
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / DELETED
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50)
);

-- ── 파일 (본문 추출 텍스트 포함) ──
CREATE TABLE tn_file (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name    VARCHAR(300)  NOT NULL,
    file_ext     VARCHAR(20),                    -- pdf, hwp, docx ...
    file_size    BIGINT,
    file_path    VARCHAR(500)  NOT NULL,
    extract_text TEXT,                           -- 파일 본문 추출 텍스트 (색인 대상)
    ref_type     VARCHAR(20),                    -- 첨부 출처 (CONTENT/BBS 등)
    ref_id       BIGINT,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50)
);

-- ── 게시판 ──
CREATE TABLE tn_bbs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_cd    VARCHAR(50)   NOT NULL,          -- 게시판 코드 (notice, faq ...)
    title       VARCHAR(500)  NOT NULL,
    content     TEXT          NOT NULL,
    writer      VARCHAR(100),
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50)
);

-- ── 메뉴 ──
CREATE TABLE tn_menu (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    menu_name   VARCHAR(200)  NOT NULL,
    menu_path   VARCHAR(300)  NOT NULL,          -- 이동 URL
    description VARCHAR(500),                    -- 메뉴 설명 (색인 보조)
    use_yn      CHAR(1)       NOT NULL DEFAULT 'Y',
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50)
);


-- ════════════════════════════════════════════════════════════
-- 원본: src/main/resources/db/migration/V3__search_view.sql
-- ════════════════════════════════════════════════════════════

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


-- ════════════════════════════════════════════════════════════
-- 원본: src/main/resources/db/migration/V4__sample_data.sql
-- ════════════════════════════════════════════════════════════

-- ============================================================
-- V4__sample_data.sql : 개발용 샘플 데이터 (각 테이블 10건)
-- 대상: tn_content / tn_file / tn_bbs / tn_menu
--       tn_search_dic_word / tn_search_dic_synonym / tn_search_recommend_keyword / tn_search_dic_banned
-- 공통: 모든 INSERT는 감사 컬럼(created_ip, created_by)을 포함한다
-- ============================================================

-- ─────────────────────────────────────────────
-- 1) 컨텐츠 (tn_content) 10건
-- ─────────────────────────────────────────────
INSERT INTO tn_content (title, content, category, status, created_ip, created_by) VALUES
('회사 소개',                 '고넷은 검색 솔루션을 개발하는 회사입니다. 형태소 분석 기반의 한국어 검색 기술을 보유하고 있습니다.', '회사', 'ACTIVE', '127.0.0.1', 'system'),
('검색엔진 서비스 안내',       'PostgreSQL 기반 검색엔진 서비스입니다. 단어사전, 동의어사전, 금지어 관리 기능을 제공합니다.', '서비스', 'ACTIVE', '127.0.0.1', 'system'),
('개인정보 처리방침',          '회사는 이용자의 개인정보를 소중히 다루며 관련 법령을 준수합니다. 수집 항목과 보유 기간을 안내합니다.', '정책', 'ACTIVE', '127.0.0.1', 'system'),
('이용약관',                  '본 약관은 검색 서비스 이용에 관한 회사와 이용자의 권리와 의무를 규정합니다.', '정책', 'ACTIVE', '127.0.0.1', 'system'),
('한국어 형태소 분석이란',     '형태소 분석은 문장을 의미를 가진 최소 단위로 분해하는 기술입니다. 노리 분석기는 한국어 처리에 널리 사용됩니다.', '기술', 'ACTIVE', '127.0.0.1', 'system'),
('동의어 사전 활용 가이드',    '동의어 사전을 활용하면 휴대폰, 핸드폰, 스마트폰처럼 다른 표현도 같은 검색 결과를 얻을 수 있습니다.', '기술', 'ACTIVE', '127.0.0.1', 'system'),
('검색 품질 개선 사례',        '무결과 검색어 분석을 통해 단어사전을 보강하여 검색 성공률을 20% 개선한 사례를 소개합니다.', '기술', 'ACTIVE', '127.0.0.1', 'system'),
('채용 안내',                 '백엔드 개발자와 데이터 엔지니어를 모집합니다. 자바와 PostgreSQL 경험자를 우대합니다.', '채용', 'ACTIVE', '127.0.0.1', 'system'),
('고객센터 이용 안내',         '평일 9시부터 18시까지 상담이 가능합니다. 자주 묻는 질문을 먼저 확인해 주세요.', '고객지원', 'ACTIVE', '127.0.0.1', 'system'),
('구버전 서비스 종료 공지',    '구버전 검색 서비스는 2026년 12월 31일에 종료됩니다. 신규 서비스로 이전해 주세요.', '공지', 'DELETED', '127.0.0.1', 'system');

-- ─────────────────────────────────────────────
-- 2) 파일 (tn_file) 10건
-- ─────────────────────────────────────────────
INSERT INTO tn_file (file_name, file_ext, file_size, file_path, extract_text, ref_type, ref_id, status, created_ip, created_by) VALUES
('검색엔진_소개서.pdf',        'pdf',  1048576, '/upload/2026/01/intro.pdf',        '검색엔진 소개서. PostgreSQL 전문검색과 노리 형태소 분석기를 결합한 아키텍처를 설명합니다.', 'CONTENT', 2, 'ACTIVE', '127.0.0.1', 'system'),
('서비스_이용가이드.pdf',      'pdf',  2097152, '/upload/2026/01/guide.pdf',        '서비스 이용 가이드. 검색창 사용법, 자동완성, 인기 검색어 기능을 안내합니다.', 'CONTENT', 2, 'ACTIVE', '127.0.0.1', 'system'),
('개인정보처리방침_v2.hwp',    'hwp',   524288, '/upload/2026/02/privacy.hwp',      '개인정보 처리방침 전문. 수집 항목, 이용 목적, 보유 기간, 파기 절차.', 'CONTENT', 3, 'ACTIVE', '127.0.0.1', 'system'),
('형태소분석_기술문서.docx',   'docx',  786432, '/upload/2026/02/nori-tech.docx',   '노리 분석기 기술 문서. 사용자 사전 등록 방법과 복합명사 분해 모드를 설명합니다.', 'CONTENT', 5, 'ACTIVE', '127.0.0.1', 'system'),
('동의어사전_양식.xlsx',       'xlsx',  262144, '/upload/2026/03/synonym-form.xlsx','동의어 사전 등록 양식. 그룹 번호, 단어, 대표어 여부 컬럼으로 구성.', 'CONTENT', 6, 'ACTIVE', '127.0.0.1', 'system'),
('2026_채용공고.pdf',          'pdf',   917504, '/upload/2026/03/recruit.pdf',      '2026년 채용 공고. 백엔드 개발자, 데이터 엔지니어 모집 요강과 지원 방법.', 'CONTENT', 8, 'ACTIVE', '127.0.0.1', 'system'),
('검색품질_개선보고서.pptx',   'pptx', 3145728, '/upload/2026/04/quality.pptx',     '검색 품질 개선 보고서. 무결과 검색어 분석과 사전 보강 프로세스.', 'CONTENT', 7, 'ACTIVE', '127.0.0.1', 'system'),
('공지_점검안내.pdf',          'pdf',   131072, '/upload/2026/05/maintenance.pdf',  '시스템 정기 점검 안내문. 점검 시간 동안 검색 서비스가 일시 중단됩니다.', 'BBS', 1, 'ACTIVE', '127.0.0.1', 'system'),
('FAQ_모음집.pdf',             'pdf',  1572864, '/upload/2026/05/faq.pdf',          '자주 묻는 질문 모음. 검색이 안 될 때 확인 사항, 금지어 정책 안내.', 'BBS', 6, 'ACTIVE', '127.0.0.1', 'system'),
('구버전_매뉴얼.pdf',          'pdf',  2621440, '/upload/2025/12/old-manual.pdf',   '구버전 검색 서비스 매뉴얼입니다.', 'CONTENT', 10, 'DELETED', '127.0.0.1', 'system');

-- ─────────────────────────────────────────────
-- 3) 게시판 (tn_bbs) 10건
-- ─────────────────────────────────────────────
INSERT INTO tn_bbs (board_cd, title, content, writer, status, created_ip, created_by) VALUES
('notice', '시스템 정기 점검 안내',          '6월 첫째 주 일요일 새벽 2시부터 4시까지 정기 점검이 진행됩니다. 점검 중 검색 서비스가 일시 중단됩니다.', '관리자', 'ACTIVE', '127.0.0.1', 'admin'),
('notice', '검색엔진 v1.0 오픈 안내',        '새로운 검색엔진이 오픈했습니다. 형태소 분석 기반으로 더 정확한 검색 결과를 제공합니다.', '관리자', 'ACTIVE', '127.0.0.1', 'admin'),
('notice', '동의어 사전 업데이트 안내',       '이번 달 동의어 사전에 신규 그룹 50건이 추가되었습니다.', '관리자', 'ACTIVE', '127.0.0.1', 'admin'),
('faq',    '검색 결과가 나오지 않아요',       '띄어쓰기를 바꾸거나 짧은 단어로 다시 검색해 보세요. 그래도 안 되면 고객센터로 문의해 주세요.', '관리자', 'ACTIVE', '127.0.0.1', 'admin'),
('faq',    '자동완성은 어떻게 동작하나요',    '입력한 글자와 유사한 색인 제목을 실시간으로 추천합니다. 두 글자 이상 입력하면 동작합니다.', '관리자', 'ACTIVE', '127.0.0.1', 'admin'),
('faq',    '금지어는 왜 검색이 안 되나요',    '운영 정책상 부적절한 단어는 검색이 차단됩니다. 이의가 있으면 문의 게시판을 이용해 주세요.', '관리자', 'ACTIVE', '127.0.0.1', 'admin'),
('free',   '검색 속도가 정말 빨라졌네요',     '이전보다 검색 결과가 훨씬 빠르고 정확하게 나옵니다. 개발자님 수고하셨습니다.', '홍길동', 'ACTIVE', '192.168.0.11', 'guest'),
('free',   '파일 내용도 검색되나요?',         '첨부된 PDF 문서의 본문 내용까지 검색되는지 궁금합니다.', '김철수', 'ACTIVE', '192.168.0.12', 'guest'),
('qna',    '인기 검색어 집계 기준 문의',      '인기 검색어는 어떤 기준으로 집계되나요? 실시간인가요?', '이영희', 'ACTIVE', '192.168.0.13', 'guest'),
('free',   '삭제된 테스트 글',               '테스트로 작성한 글입니다. 색인에서 제외되어야 합니다.', '테스터', 'DELETED', '192.168.0.14', 'guest');

-- ─────────────────────────────────────────────
-- 4) 메뉴 (tn_menu) 10건
-- ─────────────────────────────────────────────
INSERT INTO tn_menu (menu_name, menu_path, description, use_yn, created_ip, created_by) VALUES
('홈',            '/',                '메인 화면으로 이동합니다.', 'Y', '127.0.0.1', 'admin'),
('통합검색',      '/result',          '컨텐츠, 파일, 게시판, 메뉴를 한 번에 검색합니다.', 'Y', '127.0.0.1', 'admin'),
('회사소개',      '/content/1',       '회사 연혁과 비전을 소개합니다.', 'Y', '127.0.0.1', 'admin'),
('서비스안내',    '/content/2',       '검색엔진 서비스의 주요 기능을 안내합니다.', 'Y', '127.0.0.1', 'admin'),
('공지사항',      '/bbs/notice',      '시스템 공지와 업데이트 소식을 확인합니다.', 'Y', '127.0.0.1', 'admin'),
('자주묻는질문',  '/bbs/faq',         '자주 묻는 질문과 답변 모음입니다.', 'Y', '127.0.0.1', 'admin'),
('자유게시판',    '/bbs/free',        '자유롭게 의견을 나누는 공간입니다.', 'Y', '127.0.0.1', 'admin'),
('질문과답변',    '/bbs/qna',         '서비스 관련 질문을 남기고 답변을 받습니다.', 'Y', '127.0.0.1', 'admin'),
('채용안내',      '/content/8',       '진행 중인 채용 공고를 확인합니다.', 'Y', '127.0.0.1', 'admin'),
('구버전서비스',  '/legacy',          '종료 예정인 구버전 서비스 페이지입니다.', 'N', '127.0.0.1', 'admin');

-- ─────────────────────────────────────────────
-- 5) 단어사전 (tn_search_dic_word) 10건
--    Nori 사용자 사전: 신조어·고유명사·복합명사 분해
-- ─────────────────────────────────────────────
INSERT INTO tn_search_dic_word (word, segments, pos_tag, enabled, memo, created_ip, created_by) VALUES
('고넷',           NULL,              'NNP', true,  '회사명 고유명사', '127.0.0.1', 'admin'),
('노리분석기',     '노리 분석기',      'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('검색엔진',       '검색 엔진',        'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('형태소분석',     '형태소 분석',      'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('통합검색',       '통합 검색',        'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('자동완성',       '자동 완성',        'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('인기검색어',     '인기 검색어',      'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('동의어사전',     '동의어 사전',      'NNG', true,  '복합명사 분해', '127.0.0.1', 'admin'),
('무결과검색',     '무결과 검색',      'NNG', true,  '사내 용어', '127.0.0.1', 'admin'),
('풀텍스트서치',   NULL,              'NNG', false, '비활성 예시 (영문 표기 권장)', '127.0.0.1', 'admin');

-- ─────────────────────────────────────────────
-- 6) 동의어사전 (tn_search_dic_synonym) 10건
--    같은 group_id = 서로 동의어, 그룹당 1개 대표어
-- ─────────────────────────────────────────────
INSERT INTO tn_search_dic_synonym (group_id, word, is_representative, enabled, created_ip, created_by) VALUES
(1, '휴대폰',     true,  true, '127.0.0.1', 'admin'),
(1, '핸드폰',     false, true, '127.0.0.1', 'admin'),
(1, '스마트폰',   false, true, '127.0.0.1', 'admin'),
(2, '검색엔진',   true,  true, '127.0.0.1', 'admin'),
(2, '서치엔진',   false, true, '127.0.0.1', 'admin'),
(3, '문의',       true,  true, '127.0.0.1', 'admin'),
(3, '질문',       false, true, '127.0.0.1', 'admin'),
(3, '상담',       false, true, '127.0.0.1', 'admin'),
(4, '공지',       true,  true, '127.0.0.1', 'admin'),
(4, '공지사항',   false, true, '127.0.0.1', 'admin');

-- ─────────────────────────────────────────────
-- 7) 추천 검색어 (tn_search_recommend_keyword) 10건
--    관리자 등록: 상시 7건 + 기간 한정 2건 + 비활성 1건
-- ─────────────────────────────────────────────
INSERT INTO tn_search_recommend_keyword (keyword, display_order, start_date, end_date, enabled, memo, created_ip, created_by) VALUES
('검색엔진',     1, NULL,         NULL,         true,  '서비스 핵심 키워드', '127.0.0.1', 'admin'),
('형태소분석',   2, NULL,         NULL,         true,  '기술 소개 유도', '127.0.0.1', 'admin'),
('동의어사전',   3, NULL,         NULL,         true,  '기능 소개 유도', '127.0.0.1', 'admin'),
('자동완성',     4, NULL,         NULL,         true,  '기능 소개 유도', '127.0.0.1', 'admin'),
('공지사항',     5, NULL,         NULL,         true,  '게시판 유도', '127.0.0.1', 'admin'),
('자주묻는질문', 6, NULL,         NULL,         true,  'FAQ 유도', '127.0.0.1', 'admin'),
('이용가이드',   7, NULL,         NULL,         true,  '가이드 문서 유도', '127.0.0.1', 'admin'),
('채용',         8, '2026-07-01', '2026-09-30', true,  '기간 한정: 채용 시즌', '127.0.0.1', 'admin'),
('정기점검',     9, '2026-07-25', '2026-08-05', true,  '기간 한정: 점검 공지', '127.0.0.1', 'admin'),
('구버전서비스', 99, NULL,        NULL,         false, '비활성 예시 (종료 서비스)', '127.0.0.1', 'admin');

-- ─────────────────────────────────────────────
-- 8) 금지어사전 (tn_search_dic_banned) 10건
--    ※ 개발용 예시. 실제 운영 금지어는 정책에 따라 등록
-- ─────────────────────────────────────────────
INSERT INTO tn_search_dic_banned (word, block_type, enabled, memo, created_ip, created_by) VALUES
('금지어테스트1', 'BLOCK', true,  '개발 테스트용 차단어', '127.0.0.1', 'admin'),
('금지어테스트2', 'BLOCK', true,  '개발 테스트용 차단어', '127.0.0.1', 'admin'),
('욕설예시',      'BLOCK', true,  '욕설 카테고리 예시', '127.0.0.1', 'admin'),
('비방예시',      'BLOCK', true,  '비방 카테고리 예시', '127.0.0.1', 'admin'),
('도박광고',      'BLOCK', true,  '광고성 스팸 차단', '127.0.0.1', 'admin'),
('불법대출',      'BLOCK', true,  '광고성 스팸 차단', '127.0.0.1', 'admin'),
('성인광고',      'BLOCK', true,  '광고성 스팸 차단', '127.0.0.1', 'admin'),
('내부문서',      'MASK',  true,  '검색은 허용, 결과 노출 제한', '127.0.0.1', 'admin'),
('임직원명단',    'MASK',  true,  '검색은 허용, 결과 노출 제한', '127.0.0.1', 'admin'),
('구금지어',      'BLOCK', false, '비활성 예시 (해제된 금지어)', '127.0.0.1', 'admin');


-- ════════════════════════════════════════════════════════════
-- 마무리: 애플리케이션 계정(search_user) 권한 부여
-- ════════════════════════════════════════════════════════════
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA search TO search_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA search TO search_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO search_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT USAGE, SELECT ON SEQUENCES TO search_user;
-- 인기 검색어 MV 자동 갱신(REFRESH MATERIALIZED VIEW) 권한 (PostgreSQL 17+)
GRANT MAINTAIN ON search.vw_search_popular_keyword TO search_user;

-- ════════════════════════════════════════════════════════════
-- 원본: src/main/resources/db/migration/V5__table_comments.sql
-- ════════════════════════════════════════════════════════════

-- ============================================================
-- V5__table_comments.sql : 테이블·컬럼 주석 (DESIGN.md 3장)
-- ============================================================

-- ─────────────────────────────────────────────
-- 검색 대상 원본 (샘플)
-- ─────────────────────────────────────────────
COMMENT ON TABLE tn_content IS '컨텐츠 (검색 대상 원본: 페이지·아티클)';
COMMENT ON COLUMN tn_content.id          IS '컨텐츠 ID';
COMMENT ON COLUMN tn_content.title       IS '제목';
COMMENT ON COLUMN tn_content.content     IS '본문 (색인 대상)';
COMMENT ON COLUMN tn_content.category    IS '카테고리 (검색 탭 내 필터)';
COMMENT ON COLUMN tn_content.status      IS '상태 (ACTIVE=노출 / DELETED=삭제 — 색인 제외)';

COMMENT ON TABLE tn_file IS '첨부파일 (검색 대상 원본: 메타 + 본문 추출 텍스트)';
COMMENT ON COLUMN tn_file.id             IS '파일 ID';
COMMENT ON COLUMN tn_file.file_name      IS '파일명 (검색 제목으로 사용)';
COMMENT ON COLUMN tn_file.file_ext       IS '확장자 (pdf, hwp, docx ... — 검색 카테고리로 사용)';
COMMENT ON COLUMN tn_file.file_size      IS '파일 크기 (byte)';
COMMENT ON COLUMN tn_file.file_path      IS '저장 경로';
COMMENT ON COLUMN tn_file.extract_text   IS '본문 추출 텍스트 (색인 대상)';
COMMENT ON COLUMN tn_file.ref_type       IS '첨부 출처 유형 (CONTENT/BBS 등)';
COMMENT ON COLUMN tn_file.ref_id         IS '첨부 출처 ID';
COMMENT ON COLUMN tn_file.status         IS '상태 (ACTIVE / DELETED — 색인 제외)';

COMMENT ON TABLE tn_bbs IS '게시판 게시글 (검색 대상 원본)';
COMMENT ON COLUMN tn_bbs.id              IS '게시글 ID';
COMMENT ON COLUMN tn_bbs.board_cd        IS '게시판 코드 (notice, faq, free, qna — 검색 카테고리로 사용)';
COMMENT ON COLUMN tn_bbs.title           IS '제목';
COMMENT ON COLUMN tn_bbs.content         IS '본문 (색인 대상)';
COMMENT ON COLUMN tn_bbs.writer          IS '작성자';
COMMENT ON COLUMN tn_bbs.status          IS '상태 (ACTIVE / DELETED — 색인 제외)';

COMMENT ON TABLE tn_menu IS '사이트 메뉴 (검색 대상 원본)';
COMMENT ON COLUMN tn_menu.id             IS '메뉴 ID';
COMMENT ON COLUMN tn_menu.menu_name      IS '메뉴명 (검색 제목으로 사용)';
COMMENT ON COLUMN tn_menu.menu_path      IS '이동 URL (검색 결과 링크 및 카테고리 1단계 추출)';
COMMENT ON COLUMN tn_menu.description    IS '메뉴 설명 (색인 보조)';
COMMENT ON COLUMN tn_menu.use_yn         IS '사용 여부 (Y/N — N은 색인 제외)';

-- ─────────────────────────────────────────────
-- 검색 사전
-- ─────────────────────────────────────────────
COMMENT ON TABLE tn_search_dic_word IS '단어사전 — Nori 사용자 사전 (신조어·고유명사·복합명사 분해). 변경 시 전체 재색인 필요';
COMMENT ON COLUMN tn_search_dic_word.id        IS '단어 ID';
COMMENT ON COLUMN tn_search_dic_word.word      IS '단어 (한 단어로 인식시킬 표기, UNIQUE)';
COMMENT ON COLUMN tn_search_dic_word.segments  IS '복합명사 분해형 (공백 구분, NULL=단일어). 예: 검색 엔진';
COMMENT ON COLUMN tn_search_dic_word.pos_tag   IS '품사 (NNG=일반명사, NNP=고유명사)';
COMMENT ON COLUMN tn_search_dic_word.enabled   IS '활성 여부';
COMMENT ON COLUMN tn_search_dic_word.memo      IS '등록 사유 등 메모';

COMMENT ON TABLE tn_search_dic_synonym IS '동의어사전 — 같은 group_id = 서로 동의어 (검색 시 그룹 내 OR 확장, 재색인 불필요)';
COMMENT ON COLUMN tn_search_dic_synonym.id                IS '동의어 ID';
COMMENT ON COLUMN tn_search_dic_synonym.group_id          IS '동의어 그룹 번호 (같은 번호끼리 동의어)';
COMMENT ON COLUMN tn_search_dic_synonym.word              IS '단어';
COMMENT ON COLUMN tn_search_dic_synonym.is_representative IS '그룹 대표어 여부 (표기용)';
COMMENT ON COLUMN tn_search_dic_synonym.enabled           IS '활성 여부';

COMMENT ON TABLE tn_search_dic_banned IS '금지어사전 — BLOCK=검색 차단, MASK=검색식에서 제외';
COMMENT ON COLUMN tn_search_dic_banned.id         IS '금지어 ID';
COMMENT ON COLUMN tn_search_dic_banned.word       IS '금지어 (부분 문자열 매칭)';
COMMENT ON COLUMN tn_search_dic_banned.block_type IS '유형 (BLOCK=검색 자체 차단 / MASK=해당 토큰만 검색식 제외)';
COMMENT ON COLUMN tn_search_dic_banned.enabled    IS '활성 여부';
COMMENT ON COLUMN tn_search_dic_banned.memo       IS '등록 사유 등 메모';

COMMENT ON TABLE tn_search_recommend_keyword IS '추천 검색어 — 관리자 등록, 사용자 화면 추천검색어 바 노출';
COMMENT ON COLUMN tn_search_recommend_keyword.id            IS '추천 검색어 ID';
COMMENT ON COLUMN tn_search_recommend_keyword.keyword       IS '추천 키워드 (UNIQUE)';
COMMENT ON COLUMN tn_search_recommend_keyword.display_order IS '노출 순서 (낮을수록 먼저)';
COMMENT ON COLUMN tn_search_recommend_keyword.start_date    IS '노출 시작일 (NULL=상시)';
COMMENT ON COLUMN tn_search_recommend_keyword.end_date      IS '노출 종료일 (NULL=상시)';
COMMENT ON COLUMN tn_search_recommend_keyword.enabled       IS '활성 여부';
COMMENT ON COLUMN tn_search_recommend_keyword.memo          IS '등록 사유 등 메모';

-- ─────────────────────────────────────────────
-- 통합 검색 색인
-- ─────────────────────────────────────────────
COMMENT ON TABLE tn_search_index IS '통합 검색 색인 — vw_search_source를 해시 diff 동기화(매일 2회). PK=(doc_type, doc_id). 개인정보는 색인 시점에 마스킹됨';
COMMENT ON COLUMN tn_search_index.doc_type          IS '검색 대상 코드 (search.doc-types 정의: CONTENT/FILE/BBS/MENU ...)';
COMMENT ON COLUMN tn_search_index.doc_id            IS '원본 PK';
COMMENT ON COLUMN tn_search_index.title             IS '제목 (마스킹 적용, 하이라이트 대상)';
COMMENT ON COLUMN tn_search_index.summary           IS '결과 목록 출력용 본문 앞 2000자 (마스킹 적용, 발췌·하이라이트 대상)';
COMMENT ON COLUMN tn_search_index.link_url          IS '원본 이동 경로 (상대경로/http/https만 — 그 외 스킴은 # 대체)';
COMMENT ON COLUMN tn_search_index.category          IS '탭 내 카테고리 (content.category / file_ext / board_cd / 메뉴 경로 1단계)';
COMMENT ON COLUMN tn_search_index.tokens            IS 'Nori 형태소 분석 토큰 (공백 구분, keep-pos: NNG·NNP·SL·SN)';
COMMENT ON COLUMN tn_search_index.content_hash      IS '원문 변경 감지 md5 (vw_*_search 계산값) — 동기화 diff 기준';
COMMENT ON COLUMN tn_search_index.source_updated_at IS '원본 수정일 — 최신순 정렬·기간 필터·등록일 표기 기준';
COMMENT ON COLUMN tn_search_index.search_vec        IS 'FTS 벡터 (GENERATED: to_tsvector(''simple'', tokens)) — GIN 인덱스';

-- ─────────────────────────────────────────────
-- 검색 키워드 로그
-- ─────────────────────────────────────────────
COMMENT ON TABLE log_search_keyword IS '검색 키워드 로그 — 감사 컬럼이 로그 본연 컬럼을 겸함 (created_at=검색 시각, created_ip=검색자 IP, created_by=검색자 ID)';
COMMENT ON COLUMN log_search_keyword.id              IS '로그 ID';
COMMENT ON COLUMN log_search_keyword.keyword         IS '사용자 입력 원본 검색어';
COMMENT ON COLUMN log_search_keyword.analyzed_tokens IS '형태소 분석 결과 토큰 (동의어 확장 포함)';
COMMENT ON COLUMN log_search_keyword.expanded_query  IS '동의어 확장 후 최종 tsquery';
COMMENT ON COLUMN log_search_keyword.doc_type        IS '검색한 탭 (NULL=통합검색)';
COMMENT ON COLUMN log_search_keyword.result_count    IS '결과 건수 (0=무결과 — 사전 보강 단서)';
COMMENT ON COLUMN log_search_keyword.is_blocked      IS '금지어 차단 여부 (true는 인기 검색어 집계 제외)';
COMMENT ON COLUMN log_search_keyword.session_id      IS '세션 ID — 내가 찾은 검색어 1차 식별 키';
COMMENT ON COLUMN log_search_keyword.trace_id        IS '앱 로그 traceId — 로그 테이블↔앱 로그 상호 추적용 (감사 컬럼 아님, log_* 전용)';
COMMENT ON COLUMN log_search_keyword.elapsed_ms      IS '검색 처리 시간 (ms)';

-- ─────────────────────────────────────────────
-- 공통 감사 컬럼 (전 테이블 동일 의미)
-- ─────────────────────────────────────────────
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'tn_content', 'tn_file', 'tn_bbs', 'tn_menu',
        'tn_search_dic_word', 'tn_search_dic_synonym', 'tn_search_dic_banned',
        'tn_search_recommend_keyword', 'tn_search_index', 'log_search_keyword'
    ] LOOP
        EXECUTE format('COMMENT ON COLUMN %I.created_at IS ''생성일 (자동 입력)''', t);
        EXECUTE format('COMMENT ON COLUMN %I.created_ip IS ''생성자 IP (AuditInterceptor 자동 입력)''', t);
        EXECUTE format('COMMENT ON COLUMN %I.created_by IS ''생성자 ID (웹=guest, 관리화면=admin, 배치=system)''', t);
        EXECUTE format('COMMENT ON COLUMN %I.updated_at IS ''수정일 (자동 입력)''', t);
        EXECUTE format('COMMENT ON COLUMN %I.updated_ip IS ''수정자 IP (수정 시 자동 입력)''', t);
        EXECUTE format('COMMENT ON COLUMN %I.updated_by IS ''수정자 ID (수정 시 자동 입력)''', t);
    END LOOP;
END $$;

-- ─────────────────────────────────────────────
-- VIEW / MATERIALIZED VIEW
-- ─────────────────────────────────────────────
COMMENT ON VIEW vw_content_search IS '컨텐츠 색인 소스 — tn_content(ACTIVE)를 공통 8컬럼으로 변환';
COMMENT ON VIEW vw_file_search    IS '파일 색인 소스 — tn_file(ACTIVE)의 파일명+추출 텍스트를 공통 8컬럼으로 변환';
COMMENT ON VIEW vw_bbs_search     IS '게시판 색인 소스 — tn_bbs(ACTIVE)를 공통 8컬럼으로 변환';
COMMENT ON VIEW vw_menu_search    IS '메뉴 색인 소스 — tn_menu(use_yn=Y)를 공통 8컬럼으로 변환';
COMMENT ON VIEW vw_search_source  IS '색인 파이프라인 단일 진입점 — 도메인별 vw_*_search UNION ALL (새 검색 대상 추가 시 여기 등록)';
COMMENT ON MATERIALIZED VIEW vw_search_popular_keyword IS '인기 검색어 집계 (최근 7일 TOP 100, 차단 검색 제외) — 배치 집계 자산. 화면 위젯은 로그 실시간 집계 사용';


-- ════════════════════════════════════════════════════════════
-- 원본: src/main/resources/db/migration/V6__origin_path.sql
-- ════════════════════════════════════════════════════════════

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


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

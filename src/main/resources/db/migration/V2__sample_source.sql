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

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

# 검색 대상(VIEW 테이블) 추가 가이드

새로운 테이블을 검색 대상으로 추가하는 방법입니다. 색인 파이프라인은 통합 VIEW(`vw_search_source`)만
바라보므로, **핵심 작업은 "원본 테이블 + 검색 VIEW 1개 + UNION 등록"** 이고 색인·검색 코어 코드는 수정하지 않습니다.

> 예시: 뉴스 테이블 `tn_news`를 새 검색 대상 `NEWS`로 추가한다고 가정합니다.

---

## 0. 구조 이해

```
tn_news (원본)
   ↓  vw_news_search      ← 새로 만들 VIEW (공통 8컬럼으로 변환)
   ↓  vw_search_source    ← UNION ALL에 한 줄 추가
   ↓  색인 파이프라인(무수정) → tn_search_index → 검색
```

모든 검색 VIEW는 아래 **공통 8컬럼**을 반드시 같은 이름·의미로 노출해야 합니다.

| 컬럼 | 의미 | 규칙 |
|---|---|---|
| `doc_type` | 대상 구분 코드 | 대문자 고정 문자열 (예: `'NEWS'`) — 색인 PK의 일부 |
| `doc_id` | 원본 PK | BIGINT |
| `title` | 제목 | 결과 목록의 제목 (500자 이내 권장) |
| `body` | 색인 대상 원문 | 형태소 분석·요약(2000자)·하이라이트 대상 |
| `link_url` | 원본 이동 경로 | `/`로 시작하는 상대경로 또는 http(s) — 그 외 스킴은 색인 시 `#`로 대체됨 |
| `category` | 탭 내 카테고리 | 카테고리 필터·통계에 사용 (없으면 고정값 또는 NULL) |
| `updated_at` | 원본 수정일 | **최신순 정렬·기간 필터·등록일 표기 기준** — 원본 갱신 시 반드시 함께 갱신돼야 함 |
| `content_hash` | 변경 감지 md5 | **검색에 영향 주는 모든 컬럼**을 포함해 md5로 계산 |

---

## 1. 원본 테이블 준비

프로젝트 표준을 따릅니다 (CLAUDE.md 고정 규칙).

- 테이블명 접두사 `tn_`, **공통 감사 컬럼 6종** 필수
  (`created_at, created_ip NOT NULL, created_by` + `updated_at, updated_ip, updated_by`)
- 노출 여부 컬럼(예: `status`, `use_yn`)을 두고 VIEW에서 필터
- **수정 시 `updated_at` 갱신이 보장**되어야 합니다 (애플리케이션 또는 트리거) —
  이 값이 갱신되지 않으면 최신순 정렬·기간 필터가 어긋납니다

---

## 2. Flyway 마이그레이션 작성 (새 버전 파일)

⚠️ 기존 V1~V4 파일은 수정하지 않습니다. **새 버전 파일**을 추가합니다:
`src/main/resources/db/migration/V5__add_news_search.sql`

```sql
-- ============================================================
-- V5__add_news_search.sql : 뉴스 검색 대상 추가 (예시)
-- ============================================================

-- 1) 원본 테이블 (이미 존재한다면 생략)
CREATE TABLE tn_news (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(500)  NOT NULL,
    content     TEXT          NOT NULL,
    press       VARCHAR(100),                    -- 언론사 → category로 사용
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- 공통 감사 컬럼
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    created_ip  VARCHAR(45)  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    updated_ip  VARCHAR(45),
    updated_by  VARCHAR(50)
);

-- 2) 검색 VIEW — 공통 8컬럼으로 변환
CREATE VIEW vw_news_search AS
SELECT 'NEWS'                           AS doc_type,
       n.id                             AS doc_id,
       n.title                          AS title,
       n.content                        AS body,
       '/news/' || n.id                 AS link_url,
       n.press                          AS category,
       n.updated_at                     AS updated_at,
       md5(n.title || '|' || n.content || '|' || coalesce(n.press,'')) AS content_hash
FROM tn_news n
WHERE n.status = 'ACTIVE';

-- 3) 통합 소스 VIEW에 UNION 추가 (전체 재정의 — 컬럼 구조가 같아야 CREATE OR REPLACE 가능)
CREATE OR REPLACE VIEW vw_search_source AS
SELECT * FROM vw_content_search
UNION ALL SELECT * FROM vw_file_search
UNION ALL SELECT * FROM vw_bbs_search
UNION ALL SELECT * FROM vw_menu_search
UNION ALL SELECT * FROM vw_news_search;          -- ★ 추가
```

`content_hash` 작성 규칙:
- **검색·표시에 영향을 주는 모든 컬럼**을 `'|'` 구분자로 이어 md5 — 여기 빠진 컬럼은
  수정해도 해시가 같아 재색인되지 않습니다
- NULL 가능 컬럼은 `coalesce(col, '')`로 감싸기 (NULL이면 전체 결과가 NULL이 되어 해시가 깨짐)

---

## 3. 코드 반영 지점 (doc_type 등록)

색인·검색 코어는 수정할 것이 없고, **화면 라벨과 상수 목록**에만 새 doc_type을 추가합니다.

| 파일 | 위치 | 수정 내용 |
|---|---|---|
| `application.yml` | `search.result.group-order` | 전체 탭 그룹 출력 순서에 `NEWS` 추가 |
| `ObservabilityConfig.java` | `DOC_TYPES` | 색인 문서 수 게이지 대상에 추가 |
| `IndexAdmController.java` | `DOC_TYPES` | 색인 관리 현황 카드에 추가 |
| `MonitorAdmController.java` | `DOC_TYPES` | 모니터 색인 차트에 추가 |
| `templates/usr/results.html` | 검색 대상 `<select>` (약 17행) | `<option value="NEWS">뉴스</option>` |
| | 좌측 카테고리 목록 `{'ALL','CONTENT',...}` (약 137행) | 리스트에 `'NEWS'` 추가 |
| | 좌측/그룹 제목 라벨 3항 연산식 (약 141·257행) | `NEWS → '뉴스'` 라벨 분기 추가 |
| `templates/adm/index.html` | 유형 라벨 (약 20행) | 라벨 분기 추가 |
| `templates/adm/stats.html` | `typeLabel` JS 맵 (약 100행) | `NEWS: '뉴스'` 추가 |

> 참고: doc_type 라벨이 여러 곳에 흩어져 있습니다. 추가 대상이 많아지면
> 공통 유틸(메시지 소스 또는 enum)로 모으는 리팩터링을 고려하세요.

### (선택) 원본 상세 화면

검색 결과의 `link_url`(`/news/{id}`) 목적지 화면이 없으면 404가 됩니다.
`SampleViewUsrController` + `usr/view-*.html` 패턴을 참고해 상세 화면을 추가하거나,
기존 서비스의 실제 화면 URL을 `link_url`로 지정하세요.

---

## 4. 색인 반영 및 확인

1. 앱 재기동 → Flyway가 V5를 자동 적용하고, 기동 직후 동기화(sync-on-startup)가 새 문서를 색인
   - 재기동 없이 적용하려면: SQL로 V5 내용 실행 후 관리자 화면 `/adm/index` → **지금 동기화**
2. 확인 체크리스트:
   - [ ] `/adm/index` 현황 카드에 뉴스 건수 표시
   - [ ] 통합검색에서 "뉴스" 그룹 노출 + 좌측 카테고리에 건수
   - [ ] 뉴스 탭 선택 시 카테고리(언론사) 필터 표시
   - [ ] 결과의 제목·내용 하이라이트, 등록일, 원본 링크 정상
   - [ ] 최신순 정렬이 updated_at 순서와 일치
   - [ ] 원본 1건 수정 → 지금 동기화 → 해당 건만 재색인되는지 (`신규·변경 1건` 로그)
   - [ ] 원본 status를 DELETED로 변경 → 동기화 → 색인에서 제거되는지

---

## 5. 자주 하는 실수

| 증상 | 원인 |
|---|---|
| UNION 뷰 생성 오류 | 새 VIEW의 컬럼 수·이름·타입이 공통 8컬럼과 다름 (특히 doc_id는 BIGINT로 캐스팅) |
| 문서를 수정해도 재색인 안 됨 | content_hash에 해당 컬럼이 빠졌거나, 원본 updated_at이 갱신되지 않음 |
| 해시가 전부 NULL | NULL 가능 컬럼에 coalesce 누락 |
| 화면에 그룹이 안 보임 | `group-order`에 doc_type 미등록 (설정에 없는 타입은 목록 맨 뒤에 표시됨) |
| 색인 건수 카드/차트에 안 나옴 | 자바 `DOC_TYPES` 상수 3곳 미등록 |
| 결과 클릭 시 404 | link_url 목적지 화면 없음 (3장 선택 항목) |
| 링크가 `#`으로 나옴 | link_url이 상대경로/http(s)가 아님 (스킴 검증에 걸림) |

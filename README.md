# search — PostgreSQL 검색엔진

1인 개발 한국어 검색엔진. Elasticsearch 없이 **PostgreSQL 18 Full-Text Search + Lucene Nori(앱 내장)** 로 동작한다.

## 기술 스택

Spring Boot 3.5.9 · Java 21 · Maven · **MyBatis** (JPA 미사용) · PostgreSQL 18 · Thymeleaf + HTMX · Lucene Nori · Caffeine · Actuator/Prometheus/Tracing(Brave) · Flyway

## 핵심 구조

```
색인:  원본 4종(컨텐츠·파일·게시판·메뉴) → vw_*_search(VIEW) → content_hash 비교
       → Nori 형태소 분석(NNG·NNP·SL·SN) → tn_search_index (tsvector + GIN)
       └ 매일 2회 스케줄 동기화 (변경분만)
검색:  키워드 → 금지어 필터 → 형태소 분석 → 동의어 확장 → tsquery(AND/OR)
       → 탭·카테고리·기간·정렬 검색 → <mark> 하이라이트 → 로그(@Async)
```

## 주요 기능

- 통합검색: 전체 탭 카테고리별 그룹 10건 + "더보기" 상세 페이징, 무한스크롤
- 상세검색: 시작일~종료일, 결과 내 재검색(칩), AND/OR
- 사전 관리: 단어사전(Nori 사용자 사전)·동의어·금지어·추천 검색어 (DB 테이블)
- 자동완성(pg_trgm), 인기 검색어(MV 10분 자동 갱신), 내 검색어(세션/IP)
- 전 테이블 감사 컬럼 표준(created/updated × at·ip·by), traceId 로그 연동, Prometheus 메트릭

## 문서

- **[설계서 (DESIGN.md)](DESIGN.md)** — DB 스키마(Flyway V1~V4), 아키텍처, 검색 파이프라인, 화면·API 명세, 설계 결정 22항
- [샘플 데이터 (V4__sample_data.sql)](src/main/resources/db/migration/V4__sample_data.sql) — 테이블별 10건

## 실행 (로컬)

1. PostgreSQL 18 기동 후 `search` DB 생성 (`CREATE DATABASE search;`)
2. 접속 정보 설정 (택1)
   - `.env.example` → `.env` 복사 후 수정하고 환경 변수로 주입 (파일 내 안내 참조)
   - `src/main/resources/application-local.yml.example` → `application-local.yml` 복사 후 수정
3. 실행: `mvn spring-boot:run` → http://localhost:8080/search/
   (Flyway가 **search 스키마**를 만들고 V1~V4를 자동 적용, 기동 직후 샘플 데이터 색인)

앱 없이 DB만 구성하려면: [db/search_full_setup.sql](db/search_full_setup.sql)
(스키마 + 테이블/VIEW + 샘플 INSERT 통합 정리본 — Flyway로 관리할 DB에는 실행 금지)
앱 계정 권한만 부여하려면: [db/grant_search_user.sql](db/grant_search_user.sql)

## 모니터링

- **내장 대시보드**: `http://localhost:8080/search/monitor` — Chart.js(webjar 내장, **외부 연결 불필요**)
  5초 폴링. 검색 처리량·평균 응답시간(단계별 span 토글)·캐시 히트율·색인 문서 수 + 배치 현황
  (Grafana/Prometheus는 외부 연결 제한 환경을 고려해 미사용 — Actuator `/actuator/prometheus`는 열려 있어 추후 연동 가능)
- Actuator(관리 포트 9090): `http://localhost:9090/actuator/health` · `/actuator/prometheus` · `/actuator/caches`
- 검색 1건의 단계별 소요는 span 메트릭으로 확인: `search_analyze/expand/fts/highlight_seconds`
- 인덱스 검증·튜닝 노트: [docs/db-tuning.md](docs/db-tuning.md)

## 운영 팁

| 작업 | 방법 |
|---|---|
| 사전(단어/동의어/금지어/추천어) 변경 | SQL로 수정 → 앱 재시작(기동 시 사전 로드·캐시 재구성). 무재기동 리로드는 어드민(추후)에서 `DictionaryService.reloadDictionaries()` 연결 예정 |
| 색인 갱신 | 자동: 매일 06:00/18:00 해시 diff + 기동 시 1회. 품사/단어사전 변경 후엔 전체 재색인(`IndexingService.rebuildAll`, 어드민 추후) |
| 인기 검색어 | 로그 → MV 10분 자동 갱신 → 캐시 1분. 수동 갱신: `REFRESH MATERIALIZED VIEW CONCURRENTLY search.vw_search_popular_keyword;` |
| 검색 품질 점검 | `log_search_keyword`에서 `result_count = 0` 키워드 조회 → 단어/동의어 사전 보강 |

## 상태

- [x] 설계 확정 (v2.0, 2026-07)
- [x] 1단계: 기반 구축 (스캐폴딩 + Flyway + MyBatis 매퍼)
- [x] 2단계: 분석·색인 (Nori 래퍼 + 해시 diff 동기화 + 스케줄)
- [x] 3단계: 검색 코어 (금지어→동의어→tsquery→FTS→하이라이트→로그)
- [x] 4단계: UI (위젯·자동완성·상세검색 패널·무한스크롤, Tailwind v4)
- [x] 5단계: 캐시·관측성 (Caffeine 5종 + 커스텀 메트릭 + 단계별 span)
- [x] **전체 실동작 검증** — PostgreSQL 18 (postgres DB / search 스키마 / search_user), 검색·동의어 하이라이트·금지어·자동완성·인기/추천 검색어·메트릭 확인 (2026-07)
- [x] 6단계: 마무리 — 인덱스 EXPLAIN 검증, 내장 Chart.js 모니터 대시보드(/monitor), 운영 문서 (**v1.0**)
- [x] 어드민 — 사전 4종 관리(자동 리로드)·색인 관리·검색 통계 (`/search/adm/dic/word`)
- [ ] (추후) Spring Security 권한 (/adm/** 접근 제한)

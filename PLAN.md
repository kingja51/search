# PLAN.md — 개발 계획 및 진행 현황

> 설계 기준: [DESIGN.md](DESIGN.md) (v2.0, tag: design-v2.0) · 작업 규칙: [CLAUDE.md](CLAUDE.md)
> 상태 표기: ✅ 완료 · 🔄 진행 중 · ⏸ 대기 · 📌 추후

## 현재 상태 요약 (2026-07-28)

- **완료**: 설계 확정 → 1~6단계(v1.0) → 어드민 → 코드 리뷰 1차 8건 → 개인정보 마스킹 →
  파일 텍스트 추출(V6) → 코드 리뷰 2차 6건 — 전 구간 실동작 검증·푸시 완료
- **적용 스택 변화**: Lucene 10.4.0 · Tailwind CSS v4 CDN · Tika 3.3.1 + hwplib/hwpxlib
- **검증 결과** (postgres DB / search 스키마 / search_user 최소권한 계정):
  - 기동: Flyway baseline(V4) 인정 → 정상 부팅, 색인 동기화 36건(변경 없으면 diff 0건 확인)
  - 검색: "휴대폰" → 동의어(핸드폰·스마트폰) `<mark>` 하이라이트 ✓ · 금지어 차단 ✓ · 검색 로그(토큰·IP·traceId) ✓
  - 위젯: 자동완성 10건 ✓ · 추천 검색어 9건(기간 필터 정확) ✓ · 인기 검색어(로그→MV→캐시 순환) ✓
  - 관측성: 단계별 span 4종 + search.query Timer(doc_type·blocked) + 캐시 히트율 + index.documents 게이지 ✓
    (span과 Timer의 search.query 이름 충돌 발견 → span을 search.fts로 분리)
- **다음 작업**: 📌 Spring Security 권한(/adm/** 접근 제한 + 감사자 ID 교체)만 남음

---

## 1단계: 기반 구축 ✅

| 작업 | 상태 | 산출물 |
|---|---|---|
| Maven 프로젝트 생성 (com.gonet.search) | ✅ | pom.xml — Boot 3.5.9, Java 21, MyBatis, Nori, Caffeine, 관측성 |
| Flyway 마이그레이션 | ✅ | V1(사전 4종+로그) · V2(원본 4종) · V3(VIEW 5개+색인+MV) · V4(샘플 10건씩) |
| 공통 감사 처리 | ✅ | BaseEntity + AuditInterceptor(MyBatis) + ClientIpFilter/Holder |
| 도메인·매퍼 | ✅ | 도메인 POJO 11종, 매퍼 6종 + XML (mybatis/mapper/) |
| 레이아웃·메인 화면 | ✅ | layout/search/default.html, usr/main.html, SearchUsrController |
| JPA → MyBatis 전환 | ✅ | 사용자 지시로 전환 (JPA 사용 금지) |
| 앱 기동 + 스키마 적용 검증 | ✅ | 수동 설치 DB + Flyway baseline(V4)로 정상 부팅 확인 (2026-07-27) |

## 2단계: 분석·색인 ✅

| 작업 | 상태 | 산출물 |
|---|---|---|
| Nori 래퍼 (품사 keep-list NNG·NNP·SL·SN) | ✅ | analyzer/KoreanAnalyzer — MIXED 분해, LowerCaseFilter, reload() 무재기동 교체 |
| 사용자 사전 로더 | ✅ | analyzer/UserDictionaryLoader — tn_search_dic_word → UserDictionary |
| 분석기 빈 구성 | ✅ | config/NoriConfig — keep-pos 설정 외부화, 색인·검색 단일 빈 공유 |
| 색인 동기화 서비스 | ✅ | service/IndexingService — 해시 diff 3단계(추출→upsert→삭제), 청크 500, 전체 재색인 |
| 스케줄 | ✅ | 매일 2회(06:00/18:00) + 기동 시 1회(sync-on-startup) |
| 매퍼 | ✅ | SearchSourceMapper(diff/전체), SearchIndexMapper(upsertBatch/deleteOrphans) |
| 분석기 단위 테스트 | ✅ | KoreanAnalyzerTest 4건 통과 (품사 필터·복합명사·reload·빈입력) |
| 샘플 데이터 색인 실검증 | ✅ | 기동 → 색인 36건 동기화 확인 (2026-07-27) |

## 3단계: 검색 코어 ✅

| 작업 | 상태 | 산출물 |
|---|---|---|
| BannedWordService | ✅ | BLOCK 차단(부분 문자열) / MASK 토큰 제외, 요청당 스냅샷 (캐시는 5단계) |
| SynonymService | ✅ | group_id 기반 단어→그룹 확장 맵, 그룹 내 OR |
| SearchService | ✅ | 파이프라인 전체: 정규화→금지어(qPrev 재검사)→분석→확장→tsquery(AND/OR·qPrev AND)→기간(dateFrom/dateTo 우선)→FTS→하이라이트→@Async 로그 |
| 검색 매퍼 (SearchMapper) | ✅ | searchTab(정렬 쿼리 분기)·searchGrouped(row_number+type_total)·countByType·countByCategory |
| HighlightService | ✅ | escape→`<mark>`(긴 단어 우선 단일 패스)→발췌, 테스트 5건 |
| KeywordLogService | ✅ | @Async 적재(검색자 IP 선세팅), MV 10분 갱신 스케줄, 인기·내 검색어(session→IP 폴백) |
| AuditInterceptor 보완 | ✅ | created_ip/by가 이미 세팅된 경우 유지 — @Async 로그의 검색자 IP 유실 방지 |
| SearchUsrController /result | ✅ | 조건 9종 바인딩(SearchCondition), 기본 결과 화면(usr/results.html — 4단계에서 HTMX 고도화) |
| 검색 실동작 검증 | ✅ | /result?q=휴대폰 → 동의어(핸드폰) 결과·하이라이트 확인 (2026-07-27) |

## 4단계: UI ✅

| 작업 | 상태 | 산출물 |
|---|---|---|
| 검색 메인 위젯 | ✅ | 추천 검색어 칩(hx load) + 인기 검색어 TOP10(hx load) + 검색창 포커스 시 내 검색어 드롭다운 |
| 자동완성 | ✅ | AutocompleteApiController + pg_trgm similarity 쿼리, keyup 300ms 디바운스. 빈 입력=내 검색어, 2글자 미만=빈 응답 |
| 키워드 API | ✅ | KeywordApiController — /api/keyword/popular·recommend·my (fragment 응답), RecommendKeywordService |
| 결과 화면 필터 | ✅ | 정렬 토글(정확도/최신순) + 기간 버튼(전체/실시간/1일/이번주/이번달) + 개별 탭 카테고리 칩(건수 포함) |
| 상세검색 패널 | ✅ | details 토글 — (A) 시작일~종료일 + AND/OR 조건 적용 / (B) 결과 내 재검색(qPrev 누적). 조건은 전부 URL 유지 |
| qPrev 칩 | ✅ | 적용된 재검색어 칩 + × 클릭 시 해당 조건만 제거 |
| 무한스크롤 | ✅ | /result/items fragment + sentinel(hx-trigger="revealed" outerHTML 교체) |
| 위젯 fragment | ✅ | usr/keywords.html (recommend/popular/myKeywords/autocomplete/empty) |
| UI 실동작 검증 | ✅ | 검색·자동완성·위젯·칩 동작 확인 (2026-07-27) |
| 샘플 원본 뷰어 (추가) | ✅ | 검색 결과 링크 목적지 — /content/{id}, /bbs/{boardCd}(목록), /bbs/{boardCd}/{id}, /file/{id}. SampleViewUsrController + view-*.html 4종, 미존재 문서는 안내 표시 |

## 5단계: 캐시·관측성 ✅

| 작업 | 상태 | 산출물 |
|---|---|---|
| CacheConfig | ✅ | Caffeine 5종(synonyms/bannedWords/popularKeywords/recommendKeywords/autocomplete) + recordStats(). searchFirstPage는 보류(캐시 히트 시 로그 누락 → 인기검색어 왜곡 — DESIGN 5.1 명시) |
| @Cacheable 적용 | ✅ | BannedWordService·SynonymService(1엔트리 스냅샷), RecommendKeywordService, KeywordLogService.popular, AutocompleteService(신규, 접두어 키) |
| 사전 리로드 | ✅ | DictionaryService.reloadDictionaries() — 캐시 4종 evict + Nori 사전 교체. 어드민 도입 시 AFTER_COMMIT 훅에서 호출 |
| 커스텀 메트릭 | ✅ | search.query Timer(doc_type·blocked) / search.results Summary / search.blocked·noresult Counter / index.sync Timer(mode) / keyword.popular.refresh Timer / index.documents Gauge(도메인별, DB 다운 시 -1) |
| 단계별 span | ✅ | Observation API로 search.analyze/expand/query/highlight 분리 (SearchService 파이프라인 재구성) |
| trace 전파 | ✅ | ContextPropagatingTaskDecorator 빈 — @Async 로그 스레드로 traceId 전파 |
| 실동작 검증 | ✅ | :9090/actuator/prometheus에서 캐시 히트율·search.query 확인 (2026-07-27) |

## 6단계: 마무리 ✅ (v1.0)

| 작업 | 상태 | 산출물 |
|---|---|---|
| 인덱스 EXPLAIN 검증 | ✅ | 핵심 쿼리 5종 모두 의도한 인덱스 사용 확인 — [docs/db-tuning.md](docs/db-tuning.md) (운영 점검 포인트 포함: ANALYZE, GIN vacuum, 로그 파티셔닝 과제) |
| 모니터 대시보드 | ✅ | **내장 Chart.js 대시보드** `/monitor` (webjar 내장 — 외부 연결 불필요, Grafana 미사용 결정). /api/monitor/summary 5초 폴링, QPS·응답시간(span 토글)·캐시 히트율·색인 문서 수·배치 현황. 검증: 페이지 200, 메트릭 증가, 캐시 hit/miss 반영 확인 |
| README 보강 | ✅ | 모니터링·운영 팁 섹션 (사전 변경 반영, 재색인, MV 수동 갱신, 무결과 검색어 점검) |
| v1.0 태그 | ✅ | git tag v1.0 |

## 어드민 (v1.0 이후 단계) ✅ — 2026-07-28

| 작업 | 상태 | 산출물 |
|---|---|---|
| 사전 4종 관리 | ✅ | /adm/dic/{word\|synonym\|banned\|recommend} — 목록·등록·활성토글·삭제 + 사전 리로드 버튼. 변경 시 **AFTER_COMMIT에 캐시 evict + Nori 리로드 자동** (DictionaryService, 설계 5.2 이행) |
| 색인 관리 | ✅ | /adm/index — 도메인별 색인 건수, 마지막 실행 결과, 지금 동기화(diff)/전체 재색인(full) 버튼 |
| 검색 통계 | ✅ | /adm/stats — 기간 선택(7/30/90일), 총/무결과/차단 요약, 일별 검색량 막대(14일), 인기 TOP20, **무결과 검색어 TOP20**(사전 보강 단서) |
| 관리자 레이아웃 | ✅ | layout/search/admin.html (다크 사이드 메뉴), 사용자 헤더에 "관리" 링크 |
| CRUD 검증 | ✅ | 등록(guest/IP 감사) → 토글(updated_by=admin) → 삭제 실동작 확인 |
| 비고 | | 인라인 편집 대신 폼 제출(PRG) 방식 채택 — 수정은 삭제 후 재등록. 재색인 진행률 폴링은 대량 데이터 필요 시 개선 |

## 품질·보강 (어드민 이후) ✅ — 2026-07-28

| 작업 | 상태 | 산출물 |
|---|---|---|
| 코드 리뷰 1차 8건 | ✅ | 새 검색 시 qPrev 초기화, 캐시 자기호출 제거, 중복등록 배너, size/page/qPrev 클램프, XFF 미신뢰 기본, Analyzer 리로드 close 제거, 서로게이트 절단 보정, link_url 스킴 검증 |
| doc_type 단일 소스화 | ✅ | `search.doc-types` yml 맵(SearchDocTypes) — 새 검색 VIEW 추가 = Flyway VIEW + yml 한 줄 |
| 개인정보 마스킹 | ✅ | MaskingUtil **색인 시점** 적용 — 주민·외국인등록번호(전체), 카드, 휴대폰, 이메일(2자 이하 로컬파트 전체), 생년월일(라벨 문맥 기반). 뷰어는 표시 시점 보조. 패턴 변경 시 전체 재색인 |
| 테이블 주석 (V5) | ✅ | COMMENT ON 전 테이블 — 소유권 search_user 이전(grant_search_user.sql) |
| 파일 텍스트 추출 (V6) | ✅ | origin_path 컬럼 + FileExtractService — Tika(DOC~CSV)+hwplib/hwpxlib(HWP/HWPX), 스케줄 01시(최근 3일)/수동 버튼(최근 1개월), 마스킹 후 반영. `update-origin`(기본 false=색인만, true=tn_file.extract_text 저장→동기화 연쇄) |
| 코드 리뷰 2차 6건 | ✅ | **IndexJobLock 공유 락**(동기화·재색인·추출 동시 실행 방지 + 어드민 안내 배너), 추출 본문 원본 저장 옵션, 이메일 짧은 로컬파트, 외국인등록번호, 자동완성 ILIKE 이스케이프, tsquery 빈 lexeme 방어 |
| 운영 문서 | ✅ | docs/user-manual.md · admin-manual.md · add-search-source.md · db-tuning.md |

## 📌 추후

- **Spring Security 권한** — /adm/** 접근 제한 + AuditInterceptor 감사자 ID(guest/admin)를 인증 사용자로 교체
- (선택) Zipkin 연동, ShedLock(다중 인스턴스 시), log_search_keyword 파티셔닝/보관주기, 사전 인라인 편집

---

## 환경 메모

- JDK 21: `C:\Program Files\Java\jdk-21` (기본 java는 1.8 → `JAVA_HOME` 지정 필요)
- Maven 3.9.16 · Docker 없음
- **PostgreSQL 18 설치됨** (`C:\Program Files\PostgreSQL\18`, 5432 리스닝) — postgres 계정 비밀번호는 로컬 설정(.env/application-local.yml)에 기재 필요. 앱 객체는 **search 스키마** 사용(Flyway default-schema)
- 빌드: `mvn -q -DskipTests compile` · 테스트: `mvn test` · 실행: `mvn spring-boot:run`

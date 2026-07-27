# PLAN.md — 개발 계획 및 진행 현황

> 설계 기준: [DESIGN.md](DESIGN.md) (v2.0, tag: design-v2.0) · 작업 규칙: [CLAUDE.md](CLAUDE.md)
> 상태 표기: ✅ 완료 · 🔄 진행 중 · ⏸ 대기 · 📌 추후

## 현재 상태 요약 (2026-07-27)

- **완료**: 설계 확정 → 1단계(기반 구축, MyBatis 전환) → 2단계(분석·색인) → 3단계(검색 코어) → 4단계(UI 고도화)
- **적용 스택 변화**: Lucene 10.4.0 · Tailwind CSS v4 CDN
- **대기 중인 검증**: 로컬 PostgreSQL 18 미설치 → 앱 기동 + Flyway + 색인·검색·UI 실동작 확인 불가
- **다음 작업**: PostgreSQL 준비 → 기동·검색·UI 검증 → 5단계(캐시·관측성)

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
| 앱 기동 + 스키마 적용 검증 | ⏸ | **PostgreSQL 18 준비 후** — `mvn spring-boot:run` → Flyway V1~V4 적용 확인 |

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
| 샘플 데이터 색인 실검증 | ⏸ | **PostgreSQL 준비 후** — 기동 → tn_search_index 38건 내외 확인 |

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
| 검색 실동작 검증 | ⏸ | **PostgreSQL 준비 후** — /result?q=휴대폰 → 동의어(핸드폰) 결과·하이라이트 확인 |

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
| UI 실동작 검증 | ⏸ | **PostgreSQL 준비 후** — 무한스크롤·자동완성·재검색 칩 동작 확인 |

## 5단계: 캐시·관측성 ⏸

- CacheConfig: Caffeine 6종 (synonyms, bannedWords, popularKeywords, recommendKeywords, autocomplete, searchFirstPage) + recordStats()
- 사전 변경 시 AFTER_COMMIT evict + Analyzer reload 연동
- ObservabilityConfig: search.query Timer, search.noresult/blocked Counter, index.sync Timer, keyword.popular.refresh Timer, index.documents Gauge
- @Observed span 분리 (analyze/expand/query/highlight), @Async·@Scheduled trace 전파(TaskDecorator)

## 6단계: 마무리 ⏸

- 인덱스 튜닝(EXPLAIN 확인), Grafana 대시보드 4패널, README 실행 문서 보강, v1.0 태그

## 📌 추후 (v1.0 이후)

- 관리자 화면(adm/*): 사전 3종+추천어 CRUD(인라인 편집), 리로드/동기화/재색인 버튼, 검색 통계(무결과 검색어 리포트)
- Spring Security 권한 + AuditInterceptor 감사자 ID를 인증 사용자로 교체
- (선택) Zipkin 연동, ShedLock(다중 인스턴스 시)

---

## 환경 메모

- JDK 21: `C:\Program Files\Java\jdk-21` (기본 java는 1.8 → `JAVA_HOME` 지정 필요)
- Maven 3.9.16 · Docker 없음 · **PostgreSQL 18 로컬 미설치 (5432 닫힘)** ← 설치 필요
- 빌드: `mvn -q -DskipTests compile` · 테스트: `mvn test` · 실행: `mvn spring-boot:run`

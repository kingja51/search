# PLAN.md — 개발 계획 및 진행 현황

> 설계 기준: [DESIGN.md](DESIGN.md) (v2.0, tag: design-v2.0) · 작업 규칙: [CLAUDE.md](CLAUDE.md)
> 상태 표기: ✅ 완료 · 🔄 진행 중 · ⏸ 대기 · 📌 추후

## 현재 상태 요약 (2026-07-27)

- **완료**: 설계 확정 → 1단계(기반 구축, MyBatis 전환 포함) → 2단계(분석·색인)
- **대기 중인 검증**: 로컬 PostgreSQL 18 미설치 → 앱 기동 + Flyway + 색인 동기화 실동작 확인 불가
- **다음 작업**: PostgreSQL 준비 → 기동 검증 → 3단계(검색 코어)

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

## 3단계: 검색 코어 ⏸ (다음 작업)

| 작업 | 내용 |
|---|---|
| BannedWordService | 금지어 필터 (bannedWords 캐시), BLOCK 차단/MASK 처리 |
| SynonymService | 동의어 확장 (synonyms 캐시), 그룹 내 OR |
| SearchService | 파이프라인: 정규화→금지어→분석→확장→tsquery(AND/OR·qPrev)→FTS→로그 |
| 검색 매퍼 | 개별 탭 쿼리(카테고리·기간·정렬 분기) + 전체 탭 그룹 쿼리(row_number, 그룹당 10건+총건수) |
| HighlightService | escape → `<mark>` 치환(동의어 포함, 긴 단어 우선) → 발췌 |
| KeywordLogService | @Async 로그 적재, 인기 검색어 MV 10분 갱신 스케줄, 내 검색어(session→IP 폴백) |
| SearchUsrController /result | 검색 조건 9종 파라미터 바인딩, HTML fragment 응답 |

## 4단계: UI ⏸

- 검색 메인: 추천 검색어(RecommendKeywordService)·인기 검색어·내 검색어 드롭다운
- 검색 결과: 전체 탭 그룹 뷰(10건+"더보기 (N건)") / 개별 탭 무한스크롤(hx-trigger="revealed")
- 상세검색 패널: 시작일~종료일, 결과 내 재검색(qPrev 칩), AND/OR — URL 쿼리스트링 유지
- 자동완성: AutocompleteApiController + pg_trgm (hx-trigger keyup delay 300ms)
- result-item fragment: 제목·내용(2000자 발췌)·등록일·링크 + 하이라이트

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

# search — PostgreSQL 검색엔진

1인 개발 한국어 검색엔진. Elasticsearch 없이 **PostgreSQL 18 Full-Text Search + Lucene Nori(앱 내장)** 로 동작한다.

## 기술 스택

Spring Boot 3.5.9 · Java 21 · Maven · PostgreSQL 18 · Thymeleaf + HTMX · Lucene Nori · Caffeine · Actuator/Prometheus/Tracing(Brave) · Flyway

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
- [샘플 데이터 (db/V4__sample_data.sql)](db/V4__sample_data.sql) — 테이블별 10건

## 상태

- [x] 설계 확정 (v2.0, 2026-07)
- [ ] 1단계: 기반 구축 (스캐폴딩 + Flyway + 엔티티)
- [ ] 2단계: 분석·색인
- [ ] 3단계: 검색 코어
- [ ] 4단계: UI
- [ ] 5단계: 캐시·관측성
- [ ] 6단계: 마무리 (v1.0)
- [ ] (추후) 어드민·권한

# CLAUDE.md

1인 개발 한국어 검색엔진 프로젝트. PostgreSQL 18 FTS + Lucene Nori(앱 내장) 기반 — Elasticsearch 사용 안 함.

**상세 설계는 반드시 [DESIGN.md](DESIGN.md)를 참고할 것** (설계 확정 v2.0, tag: design-v2.0).
스키마·검색 파이프라인·화면·API·설계 결정 22항이 모두 그 문서에 있다. 설계와 다른 구현을 하게 될 경우
임의로 진행하지 말고 DESIGN.md를 먼저 갱신한 뒤 구현한다.

## 기술 스택

- Spring Boot 3.5.9 + Java 21 + Maven
- PostgreSQL 18 (FTS: tsvector 'simple' + GIN, pg_trgm) · Flyway (V1~V4)
- Thymeleaf + Layout Dialect + HTMX (SPA 아님, fragment 부분 렌더링)
- Lucene `lucene-analysis-nori` (형태소 분석은 자바 레이어에서 수행)
- Caffeine 캐시 · Actuator + Prometheus + Micrometer Tracing(Brave)

## 고정 규칙 (변경 금지)

### 명명
- 패키지 루트: `com.gonet.search`
- Controller 접미사: API `*ApiController` / 사용자 화면 `*UsrController` / 관리자 화면 `*AdmController`
- 테이블 접두사: 일반 `tn_` / 로그 `log_` / VIEW·MV `vw_`
- Thymeleaf 레이아웃: `templates/layout/search/` (default.html, admin.html)
- context path: `/search` — 템플릿·HTMX의 URL은 반드시 `@{...}` 표현식 사용

### 공통 감사 컬럼 (모든 tn_/log_ 테이블 필수, VIEW 제외)
`created_at, created_ip(NOT NULL), created_by` + `updated_at, updated_ip, updated_by`
- JPA 엔티티는 반드시 `BaseEntity`(@MappedSuperclass, JPA Auditing) 상속
- IP는 ClientIpHolder(ThreadLocal), ID는 AuditorAware (웹=guest, 배치=system)
- 네이티브 SQL 배치는 감사 컬럼을 직접 바인딩
- trace_id는 감사 컬럼이 아님 — log_* 테이블에만 존재

### 검색 핵심 불변식
- 색인과 검색은 **동일한 Nori Analyzer 인스턴스**를 공유한다 (토큰 불일치 방지)
- 품사는 keep-list: NNG·NNP·SL·SN만 (설정 `search.analyzer.keep-pos`, 변경 시 전체 재색인)
- 동의어 확장은 항상 그룹 내 OR, op(AND/OR)는 그룹 간 결합, qPrev(결과 내 재검색)는 항상 AND
- qPrev 포함 모든 검색어는 매 요청마다 금지어 검사를 다시 거친다
- 사전 변경 시 캐시 evict·Analyzer 리로드는 트랜잭션 커밋 후(AFTER_COMMIT)에 수행
- 하이라이트는 앱 레이어(escape 후 `<mark>`) — ts_headline 사용 금지

## 자주 쓰는 명령

```bash
mvn spring-boot:run          # 로컬 실행 (http://localhost:8080/search/)
mvn test                     # 테스트
mvn -q compile               # 빠른 컴파일 확인
```

- DB 접속 정보는 `application-local.yml`(git 미추적) 사용
- Actuator는 관리 포트 9090 (`:9090/actuator/health`)

## 작업 방식

- 커밋 메시지는 한국어, 접두어는 `feat:`/`fix:`/`docs:`/`chore:` 사용. 커밋 후 바로 push (원격: origin/main)
- Flyway 마이그레이션 파일은 수정하지 않는다 — 스키마 변경은 새 버전 파일(V5, V6, …) 추가
- 로드맵(6단계)과 진행 상태는 [README.md](README.md) 체크리스트 참조. 현재: 설계 확정, 1단계(기반 구축) 착수 전
- 관리자 화면(`adm/*`)·Spring Security 권한은 **추후 개발** — 지금 구현하지 않는다

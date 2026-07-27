# DB 인덱스 검증·튜닝 노트

> 검증일: 2026-07-27 · PostgreSQL 18.4 · postgres DB / search 스키마 · 샘플 36건 색인 기준
> 방법: `SET enable_seqscan = off` 후 EXPLAIN — 데이터가 적으면 플래너가 seq scan을 선호하므로,
> "인덱스를 탈 수 있는 쿼리 형태인가"를 검증한 것. 데이터가 쌓이면 플래너가 자동으로 인덱스를 선택한다.

## 검증 결과 — 핵심 쿼리 5종 모두 의도한 인덱스 사용 ✅

| 쿼리 패턴 | 사용 인덱스 | 실행계획 요지 |
|---|---|---|
| FTS 검색 `search_vec @@ tsquery` | `idx_search_vec` (GIN) | Bitmap Index Scan |
| 자동완성 `title % q OR title ILIKE '%q%'` | `idx_search_trgm` (GIN trgm) | **BitmapOr — 두 조건 모두** trgm 인덱스 사용 |
| 최신순 `source_updated_at >= ? ORDER BY DESC` | `idx_search_updated` | Bitmap Index Scan + Sort |
| 탭·카테고리 `doc_type = ? AND category = ?` | `idx_search_type` (복합) | Index Scan (두 조건 Index Cond) |
| 내 검색어 `created_ip = ? GROUP BY keyword` | `idx_lsk_ip` | Index Scan (ip 조건) |

## 운영 시 점검 포인트

- **통계 갱신**: 대량 색인(전체 재색인) 직후 `ANALYZE search.tn_search_index;` 권장 — 플래너 추정 정확도 확보
- **GIN 인덱스 팽창**: 색인 동기화가 잦아지면 `gin_pending_list_limit`(기본 4MB) 초과 시 검색이 느려질 수 있음
  → 대량 갱신 후 `VACUUM (ANALYZE) search.tn_search_index;`
- **ts_rank 정렬**: rank 정렬은 인덱스로 불가(값이 쿼리 의존) — GIN으로 후보를 좁힌 뒤 정렬하는 현재 구조가 정상.
  결과가 수만 건인 흔한 단어는 LIMIT 페이징으로 상위만 정렬되므로 문제 없음
- **로그 테이블 성장**: `log_search_keyword`는 무한 성장 — 월 단위 파티셔닝 또는 보관주기(예: 1년) 삭제 배치를
  데이터가 수백만 건 되기 전에 도입 (추후 과제)
- **모니터링 연계**: p95 지연은 Grafana(search.query Timer), 단계별 병목은 span(search.analyze/expand/fts/highlight)으로 추적

package com.gonet.search.mapper;

import com.gonet.search.domain.SearchKeywordLog;
import com.gonet.search.dto.KeywordStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchKeywordLogMapper {

    /** 검색 로그 적재 (감사 필드는 AuditInterceptor가 주입 — 단, IP/ID는 호출측 선세팅 우선) */
    int insert(SearchKeywordLog log);

    /** 인기 검색어 TOP N (vw_search_popular_keyword — 최근 7일 고정 집계) */
    List<KeywordStat> findPopular(@Param("limit") int limit);

    /** 기간별 인기 검색어 TOP N — 로그 실시간 집계 (fromTs=null이면 전체 기간) */
    List<KeywordStat> findPopularSince(@Param("fromTs") java.time.OffsetDateTime fromTs,
                                       @Param("limit") int limit);

    /** 내 검색어: 세션 기준 (중복 키워드 제거, 최근순) */
    List<KeywordStat> findRecentBySession(@Param("sessionId") String sessionId,
                                          @Param("limit") int limit);

    /** 내 검색어: IP 폴백 (created_ip = 검색자 IP) */
    List<KeywordStat> findRecentByIp(@Param("clientIp") String clientIp,
                                     @Param("limit") int limit);

    /** 인기 검색어 MV 갱신 (CONCURRENTLY — 조회 무중단) */
    void refreshPopular();

    /* ── 검색 통계 (어드민) ── */

    /** 일별 검색량 (최근 N일) */
    List<com.gonet.search.dto.KeyCount> countByDay(@Param("days") int days);

    /** 무결과 검색어 TOP N (최근 N일) — 사전 보강 단서 */
    List<com.gonet.search.dto.KeyCount> findNoResult(@Param("days") int days, @Param("limit") int limit);

    /** 요약: total / blocked / noresult (최근 N일) */
    java.util.Map<String, Object> statsSummary(@Param("days") int days);
}

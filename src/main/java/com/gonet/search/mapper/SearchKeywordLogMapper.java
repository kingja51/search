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

    /** 인기 검색어 TOP N (vw_search_popular_keyword) */
    List<KeywordStat> findPopular(@Param("limit") int limit);

    /** 내 검색어: 세션 기준 (중복 키워드 제거, 최근순) */
    List<KeywordStat> findRecentBySession(@Param("sessionId") String sessionId,
                                          @Param("limit") int limit);

    /** 내 검색어: IP 폴백 (created_ip = 검색자 IP) */
    List<KeywordStat> findRecentByIp(@Param("clientIp") String clientIp,
                                     @Param("limit") int limit);

    /** 인기 검색어 MV 갱신 (CONCURRENTLY — 조회 무중단) */
    void refreshPopular();
}

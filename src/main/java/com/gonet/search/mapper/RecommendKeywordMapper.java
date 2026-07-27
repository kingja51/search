package com.gonet.search.mapper;

import com.gonet.search.domain.RecommendKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RecommendKeywordMapper {

    /** 노출 대상 추천 검색어: 활성 + 노출기간(NULL=상시) 충족, display_order 순 */
    List<RecommendKeyword> findDisplayable(@Param("today") LocalDate today);

    /** 전체 목록 (어드민 — 비활성 포함) */
    List<RecommendKeyword> findAll();

    int insert(RecommendKeyword keyword);

    int toggleEnabled(@Param("id") Long id, @Param("ip") String ip);

    int deleteById(@Param("id") Long id);
}

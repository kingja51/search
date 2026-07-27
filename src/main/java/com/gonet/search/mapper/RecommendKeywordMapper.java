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
}

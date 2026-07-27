package com.gonet.search.service;

import com.gonet.search.domain.RecommendKeyword;
import com.gonet.search.mapper.RecommendKeywordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 추천 검색어 — 관리자 등록, 노출기간(start/end_date)·순서(display_order) 적용 (DESIGN.md 결정 14).
 * recommendKeywords 캐시(TTL 10분)로 서빙 — 노출기간 경계는 최대 10분 지연 허용.
 */
@Service
@RequiredArgsConstructor
public class RecommendKeywordService {

    private final RecommendKeywordMapper recommendKeywordMapper;

    /** 오늘 기준 노출 대상 추천 검색어 (display_order 순) */
    @Cacheable(cacheNames = "recommendKeywords", key = "'all'")
    public List<RecommendKeyword> displayable() {
        return recommendKeywordMapper.findDisplayable(LocalDate.now());
    }
}

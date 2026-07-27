package com.gonet.search.service;

import com.gonet.search.domain.RecommendKeyword;
import com.gonet.search.mapper.RecommendKeywordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 추천 검색어 — 관리자 등록, 노출기간(start/end_date)·순서(display_order) 적용 (DESIGN.md 결정 14).
 * ※ 5단계에서 recommendKeywords 캐시(10분) 적용 예정.
 */
@Service
@RequiredArgsConstructor
public class RecommendKeywordService {

    private final RecommendKeywordMapper recommendKeywordMapper;

    /** 오늘 기준 노출 대상 추천 검색어 (display_order 순) */
    public List<RecommendKeyword> displayable() {
        return recommendKeywordMapper.findDisplayable(LocalDate.now());
    }
}

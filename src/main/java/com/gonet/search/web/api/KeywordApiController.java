package com.gonet.search.web.api;

import com.gonet.search.config.ClientIpHolder;
import com.gonet.search.service.KeywordLogService;
import com.gonet.search.service.RecommendKeywordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 인기·추천·내 검색어 API — HTMX fragment(HTML) 응답 (DESIGN.md 8장).
 * 내 검색어는 IP 파라미터를 받지 않는다 — 서버가 요청에서 직접 추출 (설계 결정 16).
 */
@Controller
@RequestMapping("/api/keyword")
@RequiredArgsConstructor
public class KeywordApiController {

    private static final int LIMIT = 10;

    private final KeywordLogService keywordLogService;
    private final RecommendKeywordService recommendKeywordService;

    /** 인기 검색어 TOP 10 (MV 10분 자동 갱신분) */
    @GetMapping("/popular")
    public String popular(Model model) {
        model.addAttribute("popular", keywordLogService.popularKeywords(LIMIT));
        return "usr/keywords :: popular";
    }

    /** 추천 검색어 (관리자 등록, 노출기간·순서 적용) */
    @GetMapping("/recommend")
    public String recommend(Model model) {
        model.addAttribute("recommend", recommendKeywordService.displayable());
        return "usr/keywords :: recommend";
    }

    /** 내 검색어 (session 우선 → IP 폴백, 파라미터 없음) */
    @GetMapping("/my")
    public String my(HttpServletRequest request, Model model) {
        model.addAttribute("myKeywords", keywordLogService.myKeywords(
                request.getSession().getId(), ClientIpHolder.get(), LIMIT));
        return "usr/keywords :: myKeywords";
    }
}

package com.gonet.search.web.usr;

import com.gonet.search.config.SearchDocTypes;
import com.gonet.search.dto.SearchCondition;
import com.gonet.search.dto.SearchResponse;
import com.gonet.search.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 사용자 검색 화면 (메인 + 결과). HTMX 고도화(무한스크롤·상세검색 패널)는 4단계에서. */
@Controller
@RequiredArgsConstructor
public class SearchUsrController {

    private final SearchService searchService;
    private final SearchDocTypes docTypes;

    /** 메인 = 통합검색 화면 (검색어 없으면 검색 기능 소개 모드) */
    @GetMapping("/")
    public String main() {
        return "redirect:/result";
    }

    @GetMapping("/result")
    public String result(@ModelAttribute("cond") SearchCondition cond,
                         HttpServletRequest request,
                         Model model) {
        model.addAttribute("docTypes", docTypes);
        cond.applyQPrevRemove();                 // 재검색 칩 × 클릭 처리
        cond.applyWithin();                      // "결과내 재검색" 체크박스 처리 (미체크 새 검색은 qPrev 초기화)
        cond.sanitize();                         // size/page/qPrev 상한 클램프
        if (cond.getQ() == null || cond.getQ().isBlank()) {
            return "usr/results";                // 검색어 없음 → 검색 기능 소개 화면 (res 미주입)
        }
        SearchResponse res = searchService.search(cond, request.getSession().getId());
        model.addAttribute("res", res);
        return "usr/results";
    }

    /** 무한스크롤 다음 페이지 — 결과 아이템 fragment만 반환 (hx-trigger="revealed") */
    @GetMapping("/result/items")
    public String resultItems(@ModelAttribute("cond") SearchCondition cond,
                              HttpServletRequest request,
                              Model model) {
        cond.applyQPrevRemove();
        cond.sanitize();
        SearchResponse res = searchService.search(cond, request.getSession().getId());
        model.addAttribute("res", res);
        return "usr/results :: itemsPage";
    }
}

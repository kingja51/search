package com.gonet.search.web.adm;

import com.gonet.search.service.KeywordLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 검색 통계 — 기간별 검색량, 인기 검색어, 무결과 검색어(사전 보강 단서), 차단 건수.
 * ※ 권한(Spring Security)은 추후.
 */
@Controller
@RequestMapping("/adm/stats")
@RequiredArgsConstructor
public class StatsAdmController {

    private final KeywordLogService keywordLogService;

    @GetMapping
    public String stats(@RequestParam(defaultValue = "30") int days, Model model) {
        model.addAttribute("menu", "stats");
        model.addAttribute("days", days);
        model.addAttribute("summary", keywordLogService.statsSummary(days));
        var daily = keywordLogService.dailyCounts(Math.min(days, 14));
        model.addAttribute("daily", daily);
        model.addAttribute("dailyMax", daily.stream().mapToLong(com.gonet.search.dto.KeyCount::getCount).max().orElse(0));
        model.addAttribute("popular", keywordLogService.popularSinceDays(days, 20));
        model.addAttribute("noResult", keywordLogService.noResultKeywords(days, 20));
        return "adm/stats";
    }
}

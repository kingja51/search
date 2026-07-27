package com.gonet.search.web.api;

import com.gonet.search.config.ClientIpHolder;
import com.gonet.search.mapper.SearchMapper;
import com.gonet.search.service.KeywordLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 자동완성 API — pg_trgm 유사도 기반 색인 제목 추천 (DESIGN.md 8장).
 * 검색창 UX: 입력이 비어 있으면(포커스 직후) 내 검색어 드롭다운, 2글자 이상이면 자동완성.
 */
@Controller
@RequiredArgsConstructor
public class AutocompleteApiController {

    private static final int MIN_LENGTH = 2;
    private static final int LIMIT = 10;

    private final SearchMapper searchMapper;
    private final KeywordLogService keywordLogService;

    @GetMapping("/api/autocomplete")
    public String autocomplete(@RequestParam(value = "q", required = false) String q,
                               HttpServletRequest request, Model model) {
        String query = q == null ? "" : q.strip();
        if (query.isEmpty()) {
            // 포커스 직후: 내 검색어 노출
            model.addAttribute("myKeywords", keywordLogService.myKeywords(
                    request.getSession().getId(), ClientIpHolder.get(), LIMIT));
            return "usr/keywords :: myKeywords";
        }
        if (query.length() < MIN_LENGTH) {
            return "usr/keywords :: empty";
        }
        List<String> suggestions = searchMapper.autocomplete(query, LIMIT);
        model.addAttribute("suggestions", suggestions);
        return "usr/keywords :: autocomplete";
    }
}

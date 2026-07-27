package com.gonet.search.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 키워드 하이라이트 — 앱 레이어 처리, ts_headline 미사용 (DESIGN.md 4.3, 설계 결정 21).
 * 1) HTML escape (XSS 차단) → 2) 토큰+동의어를 긴 단어 우선 <mark> 치환 → 3) 첫 일치 기준 발췌.
 * 반환 문자열은 <mark> 외 전부 escape된 상태이므로 템플릿에서 th:utext로 출력한다.
 */
@Service
public class HighlightService {

    private static final int SNIPPET_RADIUS = 80;   // 첫 일치 앞뒤로 보여줄 길이

    /** 하이라이트 대상어(terms)로 패턴 생성 — 긴 단어 우선 단일 패스 치환(중첩 mark 방지) */
    public Pattern compile(Collection<String> terms) {
        List<String> sorted = terms.stream()
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .toList();
        if (sorted.isEmpty()) {
            return null;
        }
        return Pattern.compile("(" + String.join("|", sorted) + ")",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** escape 후 <mark> 하이라이트 */
    public String highlight(String text, Pattern pattern) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String escaped = escapeHtml(text);
        if (pattern == null) {
            return escaped;
        }
        return pattern.matcher(escaped).replaceAll("<mark>$1</mark>");
    }

    /** 첫 번째 일치 위치 앞뒤로 잘라낸 발췌 + 하이라이트 (일치 없으면 앞부분) */
    public String snippet(String text, Pattern pattern) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String window = text;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                int from = Math.max(0, matcher.start() - SNIPPET_RADIUS);
                int to = Math.min(text.length(), matcher.end() + SNIPPET_RADIUS * 2);
                window = (from > 0 ? "…" : "") + text.substring(from, to)
                        + (to < text.length() ? "…" : "");
            } else if (text.length() > SNIPPET_RADIUS * 3) {
                window = text.substring(0, SNIPPET_RADIUS * 3) + "…";
            }
        }
        return highlight(window, pattern);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

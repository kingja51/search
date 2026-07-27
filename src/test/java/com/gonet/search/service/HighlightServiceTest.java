package com.gonet.search.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** 하이라이트 검증 — DB 없이 실행되는 순수 단위 테스트 */
class HighlightServiceTest {

    private final HighlightService service = new HighlightService();

    @Test
    @DisplayName("키워드와 동의어가 모두 <mark>로 감싸진다")
    void highlightsTermsAndSynonyms() {
        Pattern p = service.compile(List.of("휴대폰", "핸드폰", "스마트폰"));

        String result = service.highlight("새 핸드폰과 스마트폰 비교", p);

        assertThat(result).contains("<mark>핸드폰</mark>", "<mark>스마트폰</mark>");
    }

    @Test
    @DisplayName("긴 단어 우선 매칭: 부분 중복 단어가 중첩 마킹되지 않는다")
    void longerTermWinsWithoutNesting() {
        Pattern p = service.compile(List.of("검색", "검색엔진"));

        String result = service.highlight("검색엔진 개발", p);

        assertThat(result).contains("<mark>검색엔진</mark>");
        assertThat(result).doesNotContain("<mark><mark>");
    }

    @Test
    @DisplayName("HTML은 escape되고 <mark>만 허용된다 (XSS 차단)")
    void escapesHtml() {
        Pattern p = service.compile(List.of("검색"));

        String result = service.highlight("<script>alert(1)</script> 검색", p);

        assertThat(result).contains("&lt;script&gt;");
        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("<mark>검색</mark>");
    }

    @Test
    @DisplayName("발췌: 첫 일치 위치 주변을 잘라내고 말줄임을 붙인다")
    void snippetAroundFirstMatch() {
        Pattern p = service.compile(List.of("키워드"));
        String longText = "가".repeat(300) + " 키워드 등장 " + "나".repeat(300);

        String result = service.snippet(longText, p);

        assertThat(result).contains("<mark>키워드</mark>");
        assertThat(result).startsWith("…").endsWith("…");
        assertThat(result.length()).isLessThan(longText.length());
    }

    @Test
    @DisplayName("대소문자 무시: pdf 검색어로 PDF도 강조된다")
    void caseInsensitive() {
        Pattern p = service.compile(List.of("pdf"));

        String result = service.highlight("PDF 파일 안내", p);

        assertThat(result).contains("<mark>PDF</mark>");
    }
}

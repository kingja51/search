package com.gonet.search.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 검색 응답 — 전체 탭은 groups, 개별 탭은 items 사용 */
@Getter
@Setter
@NoArgsConstructor
public class SearchResponse {

    private boolean blocked = false;            // 금지어 차단 여부
    private String message;                     // 차단·무결과 안내 문구

    private String q;
    private List<SearchGroup> groups = new ArrayList<>();          // 전체 탭: 도메인 그룹 뷰
    private List<SearchResultItem> items = new ArrayList<>();      // 개별 탭: 페이징 뷰
    private long total = 0;

    private Map<String, Long> tabCounts = new LinkedHashMap<>();   // ALL 포함 탭별 건수
    private List<KeyCount> categoryCounts = new ArrayList<>();     // 개별 탭: 카테고리별 건수

    private int page = 0;
    private int size = 10;
    private boolean hasMore = false;

    private long elapsedMs = 0;

    public static SearchResponse blocked(String q) {
        SearchResponse res = new SearchResponse();
        res.blocked = true;
        res.q = q;
        res.message = "검색할 수 없는 단어가 포함되어 있습니다.";
        return res;
    }

    public static SearchResponse empty(String q, String message) {
        SearchResponse res = new SearchResponse();
        res.q = q;
        res.message = message;
        return res;
    }
}

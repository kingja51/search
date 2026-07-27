package com.gonet.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 전체 탭의 카테고리(도메인)별 그룹 — 그룹당 10건 + 총건수 (DESIGN.md 4.3) */
@Getter
@AllArgsConstructor
public class SearchGroup {

    private final String docType;               // CONTENT / FILE / BBS / MENU
    private final long total;                   // 그룹 총건수 ("더보기 (N건)" 표시)
    private final List<SearchResultItem> items; // 상위 N건 (기본 10)

    public boolean isHasMore() {
        return total > items.size();
    }
}

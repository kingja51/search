package com.gonet.search.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 키워드 통계 1행 — 인기 검색어(searchCount 포함) / 내 검색어(searchCount NULL) */
@Getter
@Setter
@NoArgsConstructor
public class KeywordStat {

    private String keyword;
    private Long searchCount;
    private OffsetDateTime lastSearchedAt;
}

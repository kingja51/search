package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 추천 검색어 — 관리자 등록, 노출 기간·순서 통제 (tn_search_recommend_keyword) */
@Getter
@Setter
@NoArgsConstructor
public class RecommendKeyword extends BaseEntity {

    private Long id;
    private String keyword;
    private int displayOrder = 0;               // 노출 순서 (낮을수록 먼저)
    private LocalDate startDate;                // 노출 시작일 (NULL=상시)
    private LocalDate endDate;                  // 노출 종료일 (NULL=상시)
    private boolean enabled = true;
    private String memo;
}

package com.gonet.search.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 추천 검색어 — 관리자 등록, 노출 기간·순서 통제 */
@Entity
@Table(name = "tn_search_recommend_keyword")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;               // 노출 순서 (낮을수록 먼저)

    @Column(name = "start_date")
    private LocalDate startDate;                // 노출 시작일 (NULL=상시)

    @Column(name = "end_date")
    private LocalDate endDate;                  // 노출 종료일 (NULL=상시)

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 300)
    private String memo;
}

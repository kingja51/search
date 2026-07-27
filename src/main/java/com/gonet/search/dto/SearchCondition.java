package com.gonet.search.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 검색 조건 파라미터 (DESIGN.md 4.3 — /result 쿼리스트링 바인딩) */
@Getter
@Setter
@NoArgsConstructor
public class SearchCondition {

    private String q;                           // 검색어 (공백 구분 다중)
    private String type = "ALL";                // ALL / CONTENT / FILE / BBS / MENU
    private String category;                    // 탭 내 카테고리 (NULL=전체)
    private String sort = "accuracy";           // accuracy(정확도) / latest(최신순)
    private String period = "all";              // 6h / 1d / week / month / all
    private String op = "AND";                  // AND / OR (검색어 그룹 간 결합)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;                 // 상세검색: 시작일 (지정 시 period 무시)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;                   // 상세검색: 종료일

    private List<String> qPrev = new ArrayList<>();  // 결과 내 재검색 (이전 검색어, 항상 AND)

    private int page = 0;
    private int size = 10;

    public boolean isAllTab() {
        return type == null || type.isBlank() || "ALL".equalsIgnoreCase(type);
    }

    public int offset() {
        return Math.max(page, 0) * size;
    }
}

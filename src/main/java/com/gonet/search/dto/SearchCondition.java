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

    private String qPrevRemove;                 // 칩 × 클릭: 제거할 qPrev 항목 (서버에서 제거 처리)

    private boolean within = false;             // "결과내 재검색" 체크박스
    private String withinBase;                  // 체크 시 qPrev로 쌓을 직전 검색어 (hidden)

    private int page = 0;
    private int size = 10;

    /** qPrevRemove로 지정된 재검색 조건을 목록에서 제거 (칩 × 클릭 처리 — 컨트롤러 진입 시 호출) */
    public void applyQPrevRemove() {
        if (qPrevRemove != null && !qPrevRemove.isBlank()) {
            qPrev.removeIf(qPrevRemove::equals);
            qPrevRemove = null;
        }
    }

    /** "결과내 재검색" 체크: 직전 검색어(withinBase)를 qPrev에 누적 (컨트롤러 진입 시 호출) */
    public void applyWithin() {
        if (within && withinBase != null && !withinBase.isBlank()
                && !withinBase.equals(q) && !qPrev.contains(withinBase)) {
            qPrev.add(withinBase);
        }
        within = false;
        withinBase = null;
    }

    public boolean isAllTab() {
        return type == null || type.isBlank() || "ALL".equalsIgnoreCase(type);
    }

    public int offset() {
        return Math.max(page, 0) * size;
    }
}

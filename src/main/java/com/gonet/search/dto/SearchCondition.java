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

    private static final int MAX_SIZE = 50;      // 페이지 크기 상한 (URL 조작 방어)
    private static final int MAX_PAGE = 1000;
    private static final int MAX_QPREV = 5;      // 결과내 재검색 누적 상한 (tsquery 비대 방지)

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

    /**
     * "결과내 재검색" 체크박스 처리 (컨트롤러 진입 시 호출).
     * withinBase 파라미터는 상단 검색바 폼 제출에만 존재한다:
     * - 체크됨   → 직전 검색어(withinBase)를 qPrev에 누적 (좁혀 가기)
     * - 체크 안 됨 → 새 검색이므로 qPrev 초기화 (이전 재검색 조건이 따라오지 않게)
     * 탭/정렬/칩 등 링크 이동에는 withinBase가 없으므로 qPrev가 유지된다.
     */
    public void applyWithin() {
        if (withinBase != null) {
            if (within && !withinBase.isBlank()
                    && !withinBase.equals(q) && !qPrev.contains(withinBase)) {
                qPrev.add(withinBase);
            } else if (!within) {
                qPrev.clear();
            }
        }
        within = false;
        withinBase = null;
    }

    /** 파라미터 상한 클램프 — URL 직접 조작 방어 (컨트롤러 진입 시 호출) */
    public void sanitize() {
        if (size < 1) {
            size = 10;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (page < 0) {
            page = 0;
        } else if (page > MAX_PAGE) {
            page = MAX_PAGE;
        }
        if (qPrev.size() > MAX_QPREV) {          // 최근 조건 5개만 유지
            qPrev = new ArrayList<>(qPrev.subList(qPrev.size() - MAX_QPREV, qPrev.size()));
        }
    }

    public boolean isAllTab() {
        return type == null || type.isBlank() || "ALL".equalsIgnoreCase(type);
    }

    public int offset() {
        return Math.max(page, 0) * size;
    }
}

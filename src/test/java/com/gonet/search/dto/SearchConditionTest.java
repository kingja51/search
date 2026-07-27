package com.gonet.search.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 검색 조건 전처리(결과내 재검색·클램프) 검증 — DB 없이 실행되는 순수 단위 테스트 */
class SearchConditionTest {

    @Test
    @DisplayName("체크 안 된 새 검색(상단 검색바)은 qPrev를 초기화한다")
    void freshSearchClearsQPrev() {
        SearchCondition cond = new SearchCondition();
        cond.setQ("새검색어");
        cond.setQPrev(new ArrayList<>(List.of("이전1", "이전2")));
        cond.setWithin(false);
        cond.setWithinBase("직전검색어");        // 상단 검색바 폼 제출 표식

        cond.applyWithin();

        assertThat(cond.getQPrev()).isEmpty();
    }

    @Test
    @DisplayName("결과내 재검색 체크 시 직전 검색어가 qPrev에 누적된다")
    void withinAddsBaseToQPrev() {
        SearchCondition cond = new SearchCondition();
        cond.setQ("케이스");
        cond.setQPrev(new ArrayList<>(List.of("휴대폰")));
        cond.setWithin(true);
        cond.setWithinBase("방수");

        cond.applyWithin();

        assertThat(cond.getQPrev()).containsExactly("휴대폰", "방수");
    }

    @Test
    @DisplayName("링크 이동(withinBase 없음)은 qPrev를 유지한다")
    void linkNavigationKeepsQPrev() {
        SearchCondition cond = new SearchCondition();
        cond.setQ("케이스");
        cond.setQPrev(new ArrayList<>(List.of("휴대폰")));

        cond.applyWithin();

        assertThat(cond.getQPrev()).containsExactly("휴대폰");
    }

    @Test
    @DisplayName("sanitize: size·page 상한 클램프, qPrev는 최근 5개만 유지")
    void sanitizeClampsParams() {
        SearchCondition cond = new SearchCondition();
        cond.setSize(99999);
        cond.setPage(-3);
        cond.setQPrev(new ArrayList<>(List.of("a", "b", "c", "d", "e", "f", "g")));

        cond.sanitize();

        assertThat(cond.getSize()).isEqualTo(50);
        assertThat(cond.getPage()).isZero();
        assertThat(cond.getQPrev()).containsExactly("c", "d", "e", "f", "g");
    }

    @Test
    @DisplayName("sanitize: size 0 이하는 기본값 10")
    void sanitizeRestoresDefaultSize() {
        SearchCondition cond = new SearchCondition();
        cond.setSize(0);

        cond.sanitize();

        assertThat(cond.getSize()).isEqualTo(10);
    }
}

package com.gonet.search.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 개인정보 마스킹 검증 — DB 없이 실행되는 순수 단위 테스트 */
class MaskingUtilTest {

    @Test
    @DisplayName("주민등록번호: 뒤 6자리 마스킹 (구분자 유무 모두)")
    void maskResidentRegistrationNumber() {
        assertThat(MaskingUtil.mask("주민번호 990101-1234567 입니다"))
                .isEqualTo("주민번호 990101-1****** 입니다");
        assertThat(MaskingUtil.mask("9901012234567"))
                .isEqualTo("990101-2******");
    }

    @Test
    @DisplayName("휴대폰번호: 중간 자리 마스킹 (하이픈/공백/붙여쓰기, 구형 3자리)")
    void maskMobileNumber() {
        assertThat(MaskingUtil.mask("연락처 010-1234-5678"))
                .isEqualTo("연락처 010-****-5678");
        assertThat(MaskingUtil.mask("01012345678"))
                .isEqualTo("010-****-5678");
        assertThat(MaskingUtil.mask("011 123 4567"))
                .isEqualTo("011-****-4567");
    }

    @Test
    @DisplayName("카드번호: 가운데 8자리 마스킹")
    void maskCardNumber() {
        assertThat(MaskingUtil.mask("카드 1234-5678-9012-3456 결제"))
                .isEqualTo("카드 1234-****-****-3456 결제");
        assertThat(MaskingUtil.mask("1234567890123456"))
                .isEqualTo("1234-****-****-3456");
    }

    @Test
    @DisplayName("이메일: 로컬파트 앞 2자만 유지")
    void maskEmail() {
        assertThat(MaskingUtil.mask("문의: hong.gildong@example.com"))
                .isEqualTo("문의: ho****@example.com");
    }

    @Test
    @DisplayName("혼합 문장: 여러 개인정보를 한 번에 마스킹")
    void maskMixedText() {
        String input = "홍길동(990101-1234567), 전화 010-1234-5678, 카드 1111-2222-3333-4444, mail hong@test.co.kr";
        String result = MaskingUtil.mask(input);

        assertThat(result).contains("990101-1******");
        assertThat(result).contains("010-****-5678");
        assertThat(result).contains("1111-****-****-4444");
        assertThat(result).contains("ho****@test.co.kr");
        assertThat(result).doesNotContain("1234567", "2222", "3333", "hong@");
    }

    @Test
    @DisplayName("오탐 방지: 일반 숫자·날짜·짧은 번호는 건드리지 않는다")
    void doesNotMaskOrdinaryNumbers() {
        assertThat(MaskingUtil.mask("2026년 7월 28일")).isEqualTo("2026년 7월 28일");
        assertThat(MaskingUtil.mask("접수번호 20260728")).isEqualTo("접수번호 20260728");
        assertThat(MaskingUtil.mask("내선 1234-5678")).isEqualTo("내선 1234-5678");
        assertThat(MaskingUtil.mask("아이폰 15")).isEqualTo("아이폰 15");
        assertThat(MaskingUtil.mask(null)).isNull();
        assertThat(MaskingUtil.mask("")).isEmpty();
    }
}

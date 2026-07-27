package com.gonet.search.util;

import java.util.regex.Pattern;

/**
 * 개인정보 마스킹 유틸 (DESIGN.md 4.4 개인정보 마스킹).
 *
 * 적용 시점:
 * - **색인 시점(주)**: IndexingService가 title/body를 마스킹한 뒤 색인 —
 *   tn_search_index(title·summary·tokens)에 개인정보가 저장되지 않으며,
 *   검색·자동완성·하이라이트 등 모든 검색 경로가 자동으로 보호된다.
 * - **표시 시점(보조)**: 색인을 거치지 않는 샘플 뷰어 화면(원본 직접 출력)에 적용.
 *
 * ※ 마스킹 패턴을 변경하면 기존 색인에는 반영되지 않으므로 **전체 재색인**이 필요하다.
 * ※ 계좌번호는 은행별 형식이 제각각이라 오탐(일반 숫자열 훼손) 위험이 커 제외 —
 *   필요 시 형식을 확정해 패턴을 추가할 것.
 */
public final class MaskingUtil {

    /** 주민등록번호: 990101-1234567 → 990101-1****** (뒤 6자리 마스킹, 성별자리 유지) */
    private static final Pattern RRN =
            Pattern.compile("(?<!\\d)(\\d{6})[-\\s]?([1-4])\\d{6}(?!\\d)");

    /** 카드번호(4-4-4-4, 구분자 유무): 1234-5678-9012-3456 → 1234-****-****-3456 */
    private static final Pattern CARD =
            Pattern.compile("(?<!\\d)(\\d{4})[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?(\\d{4})(?!\\d)");

    /** 휴대폰번호(01X, 구분자 유무): 010-1234-5678 → 010-****-5678 */
    private static final Pattern MOBILE =
            Pattern.compile("(?<!\\d)(01[016789])[-\\s]?\\d{3,4}[-\\s]?(\\d{4})(?!\\d)");

    /** 이메일: hong.gildong@example.com → ho****@example.com (로컬파트 앞 2자만 유지) */
    private static final Pattern EMAIL =
            Pattern.compile("([A-Za-z0-9._%+-]{1,2})[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private MaskingUtil() {
    }

    /**
     * 텍스트 내 개인정보를 검사해 마스킹한다.
     * 적용 순서: 주민번호 → 카드번호 → 휴대폰 → 이메일
     * (긴 숫자 패턴을 먼저 처리해 부분 오매칭을 방지)
     */
    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = RRN.matcher(text).replaceAll("$1-$2******");
        masked = CARD.matcher(masked).replaceAll("$1-****-****-$2");
        masked = MOBILE.matcher(masked).replaceAll("$1-****-$2");
        masked = EMAIL.matcher(masked).replaceAll("$1****@$2");
        return masked;
    }
}

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

    /**
     * 주민등록번호: 990101-1234567 → ******-*******
     * 앞 6자리(=생년월일)까지 전체 마스킹 — 생년월일을 민감정보로 취급하는 정책과 일관성 유지.
     * 뒷자리 첫 숫자 1~4=내국인, 5~8=외국인등록번호 — 둘 다 마스킹.
     */
    private static final Pattern RRN =
            Pattern.compile("(?<!\\d)\\d{6}[-\\s]?[1-8]\\d{6}(?!\\d)");

    /**
     * 생년월일: **라벨 문맥 기반** — "생년월일/생일/출생일" 등 라벨이 바로 앞에 있는 날짜만 마스킹.
     * (날짜 형태만으로는 공고일·마감일 등 일반 날짜와 구분이 불가능하므로 라벨 없는 날짜는 건드리지 않는다)
     * 지원 형식: YYYY-MM-DD · YYYY.MM.DD · YYYY/MM/DD · YYYY년 M월 D일 · YYMMDD(6자리)
     * 예) "생년월일: 1990-01-01" → "생년월일: ****-**-**" / "공고일: 2026-07-28" → 유지
     */
    private static final Pattern BIRTH_DATE = Pattern.compile(
            "(생년월일|출생년월일|출생일|생년|생일)"          // 라벨 (긴 것 우선)
            + "(\\s*[:：]?\\s*)"                              // 구분자
            + "((?:\\d{4}[-./]\\d{1,2}[-./]\\d{1,2})"        //   YYYY-MM-DD, YYYY.M.D, YYYY/MM/DD
            + "|(?:\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일)"    //   YYYY년 M월 D일
            + "|(?:\\d{6}(?![-\\s]?\\d)))");                  //   YYMMDD — 주민번호 앞부분은 제외(RRN이 전체 처리)

    /** 카드번호(4-4-4-4, 구분자 유무): 1234-5678-9012-3456 → 1234-****-****-3456 */
    private static final Pattern CARD =
            Pattern.compile("(?<!\\d)(\\d{4})[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?(\\d{4})(?!\\d)");

    /** 휴대폰번호(01X, 구분자 유무): 010-1234-5678 → 010-****-5678 */
    private static final Pattern MOBILE =
            Pattern.compile("(?<!\\d)(01[016789])[-\\s]?\\d{3,4}[-\\s]?(\\d{4})(?!\\d)");

    /**
     * 이메일: hong.gildong@example.com → ho****@example.com (로컬파트 앞 2자만 유지).
     * 로컬파트가 2자 이하면 전체를 * 치환 (ab@naver.com → **@naver.com — 앞자리 유지 시 원본이 그대로 노출됨)
     */
    private static final Pattern EMAIL =
            Pattern.compile("([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private MaskingUtil() {
    }

    /**
     * 텍스트 내 개인정보를 검사해 마스킹한다.
     * 적용 순서: 생년월일(라벨 문맥) → 주민번호 → 카드번호 → 휴대폰 → 이메일
     * (긴 숫자 패턴을 먼저 처리해 부분 오매칭을 방지)
     */
    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        // 생년월일: 라벨은 남기고 날짜의 숫자만 * 치환 (구분자·년월일 표기는 유지)
        String masked = BIRTH_DATE.matcher(text).replaceAll(mr ->
                mr.group(1) + mr.group(2) + mr.group(3).replaceAll("\\d", "*"));
        masked = RRN.matcher(masked).replaceAll("******-*******");
        masked = CARD.matcher(masked).replaceAll("$1-****-****-$2");
        masked = MOBILE.matcher(masked).replaceAll("$1-****-$2");
        masked = EMAIL.matcher(masked).replaceAll(mr -> {
            String local = mr.group(1);
            String head = local.length() <= 2 ? "*".repeat(local.length()) : local.substring(0, 2) + "****";
            return head + "@" + mr.group(2);
        });
        return masked;
    }
}

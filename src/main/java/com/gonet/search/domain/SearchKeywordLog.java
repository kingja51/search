package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 검색 키워드 로그 (log_search_keyword).
 * 감사 필드가 로그 본연의 의미를 겸한다: createdAt=검색 시각, createdIp=검색자 IP, createdBy=검색자 ID.
 */
@Getter
@Setter
@NoArgsConstructor
public class SearchKeywordLog extends BaseEntity {

    private Long id;
    private String keyword;                     // 사용자가 입력한 원본
    private String analyzedTokens;              // 형태소 분석 결과 토큰
    private String expandedQuery;               // 동의어 확장 후 최종 tsquery
    private String docType;                     // 검색한 탭 (NULL=전체)
    private int resultCount = 0;
    private boolean blocked = false;            // is_blocked
    private String sessionId;                   // "내 검색어" 1차 식별 키
    private String traceId;                     // 앱 로그(traceId)와 상호 추적용
    private Integer elapsedMs;
}

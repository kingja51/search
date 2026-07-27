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

/**
 * 검색 키워드 로그.
 * 감사 컬럼이 로그 본연의 의미를 겸한다: created_at=검색 시각, created_ip=검색자 IP, created_by=검색자 ID.
 */
@Entity
@Table(name = "log_search_keyword")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeywordLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String keyword;                     // 사용자가 입력한 원본

    @Column(name = "analyzed_tokens", length = 500)
    private String analyzedTokens;              // 형태소 분석 결과 토큰

    @Column(name = "expanded_query", length = 1000)
    private String expandedQuery;               // 동의어 확장 후 최종 tsquery

    @Column(name = "doc_type", length = 20)
    private String docType;                     // 검색한 탭 (NULL=전체)

    @Column(name = "result_count", nullable = false)
    private int resultCount = 0;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "session_id", length = 64)
    private String sessionId;                   // "내 검색어" 1차 식별 키

    @Column(name = "trace_id", length = 32)
    private String traceId;                     // 앱 로그(traceId)와 상호 추적용

    @Column(name = "elapsed_ms")
    private Integer elapsedMs;
}

package com.gonet.search.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/** 검색 결과 1건 — 기본 출력 필드: 제목·내용(발췌)·등록일·링크 (DESIGN.md 4.3) */
@Getter
@Setter
@NoArgsConstructor
public class SearchResultItem {

    private static final DateTimeFormatter REG_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private String docType;
    private Long docId;
    private String title;                       // 하이라이트 적용 후 <mark> 포함 (escape 완료)
    private String summary;                     // 키워드 주변 발췌 + 하이라이트 (escape 완료)
    private String linkUrl;
    private String category;
    private OffsetDateTime sourceUpdatedAt;
    private Double rank;                        // ts_rank (정확도)
    private Integer rn;                         // 그룹 쿼리: 그룹 내 순번
    private Long typeTotal;                     // 그룹 쿼리: 도메인별 총건수 ("더보기 (N건)")

    /** 등록일 표기 (yyyy.MM.dd) */
    public String getRegDate() {
        return sourceUpdatedAt == null ? "" : REG_DATE.format(sourceUpdatedAt);
    }
}

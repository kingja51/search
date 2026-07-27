package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 통합 검색 색인 (tn_search_index). PK = (docType, docId) 복합키.
 * search_vec(tsvector)은 DB GENERATED 컬럼이므로 도메인에 두지 않는다.
 * 색인 배치는 매퍼의 upsert SQL을 사용한다 (DESIGN.md 4.4).
 */
@Getter
@Setter
@NoArgsConstructor
public class SearchIndex extends BaseEntity {

    private String docType;                     // CONTENT / FILE / BBS / MENU
    private Long docId;
    private String title;
    private String summary;                     // 결과 목록 출력용 본문 (body 앞 2000자)
    private String linkUrl;
    private String category;
    private String tokens;                      // Nori 분석 결과 (공백 구분)
    private String contentHash;
    private OffsetDateTime sourceUpdatedAt;     // 원본 수정일 (정렬·기간·등록일 표기 기준)
}

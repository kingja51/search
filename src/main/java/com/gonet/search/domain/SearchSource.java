package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 색인 소스 1건 — vw_search_source(4개 vw_*_search의 UNION) 조회 결과.
 * VIEW 조회 전용이므로 감사 필드(BaseEntity)를 상속하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SearchSource {

    private String docType;                     // CONTENT / FILE / BBS / MENU
    private Long docId;
    private String title;
    private String body;                        // 색인 대상 원문
    private String linkUrl;
    private String category;
    private OffsetDateTime updatedAt;           // 원본 수정일
    private String contentHash;                 // 변경 감지용 md5
}

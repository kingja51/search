package com.gonet.search.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 통합 검색 색인 테이블.
 * search_vec(tsvector)은 DB GENERATED 컬럼이므로 엔티티에 매핑하지 않는다.
 * 색인 배치는 네이티브 upsert SQL을 사용한다 (DESIGN.md 4.4).
 */
@Entity
@Table(name = "tn_search_index")
@IdClass(SearchIndexId.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchIndex extends BaseEntity {

    @Id
    @Column(name = "doc_type", length = 20)
    private String docType;                     // CONTENT / FILE / BBS / MENU

    @Id
    @Column(name = "doc_id")
    private Long docId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String summary;                     // 결과 목록 출력용 본문 (body 앞 2000자)

    @Column(name = "link_url", nullable = false, length = 500)
    private String linkUrl;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, columnDefinition = "text")
    private String tokens;                      // Nori 분석 결과 (공백 구분)

    @Column(name = "content_hash", nullable = false, length = 32)
    private String contentHash;

    @Column(name = "source_updated_at", nullable = false)
    private OffsetDateTime sourceUpdatedAt;     // 원본 수정일 (정렬·기간·등록일 표기 기준)
}

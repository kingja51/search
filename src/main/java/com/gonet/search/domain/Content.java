package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 컨텐츠 (검색 대상 원본, tn_content) */
@Getter
@Setter
@NoArgsConstructor
public class Content extends BaseEntity {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String status = "ACTIVE";           // ACTIVE / DELETED
}

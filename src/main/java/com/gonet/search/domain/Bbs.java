package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 게시판 게시글 (검색 대상 원본, tn_bbs) */
@Getter
@Setter
@NoArgsConstructor
public class Bbs extends BaseEntity {

    private Long id;
    private String boardCd;                     // notice, faq, free, qna ...
    private String title;
    private String content;
    private String writer;
    private String status = "ACTIVE";
}

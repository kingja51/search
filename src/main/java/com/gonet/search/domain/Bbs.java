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

/** 게시판 게시글 (검색 대상 원본) */
@Entity
@Table(name = "tn_bbs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bbs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_cd", nullable = false, length = 50)
    private String boardCd;                     // notice, faq, free, qna ...

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 100)
    private String writer;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}

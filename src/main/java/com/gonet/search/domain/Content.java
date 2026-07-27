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

/** 컨텐츠 (검색 대상 원본) */
@Entity
@Table(name = "tn_content")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";           // ACTIVE / DELETED
}

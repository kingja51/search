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

/** 단어사전 — Nori 사용자 사전 (신조어·고유명사·복합명사 분해) */
@Entity
@Table(name = "tn_search_dic_word")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DicWord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;                        // 예: '아이폰15'

    @Column(length = 200)
    private String segments;                    // 복합명사 분해형 (NULL이면 단일어)

    @Column(name = "pos_tag", length = 20)
    private String posTag = "NNG";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 300)
    private String memo;
}

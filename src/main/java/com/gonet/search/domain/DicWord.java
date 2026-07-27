package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 단어사전 — Nori 사용자 사전 (tn_search_dic_word) */
@Getter
@Setter
@NoArgsConstructor
public class DicWord extends BaseEntity {

    private Long id;
    private String word;                        // 예: '아이폰15'
    private String segments;                    // 복합명사 분해형 (NULL이면 단일어)
    private String posTag = "NNG";
    private boolean enabled = true;
    private String memo;
}

package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 동의어사전 — 같은 groupId = 서로 동의어 (tn_search_dic_synonym) */
@Getter
@Setter
@NoArgsConstructor
public class DicSynonym extends BaseEntity {

    private Long id;
    private Long groupId;
    private String word;
    private boolean representative = false;     // 대표어 여부 (is_representative)
    private boolean enabled = true;
}

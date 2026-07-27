package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 금지어사전 — BLOCK(검색차단) / MASK(결과숨김) (tn_search_dic_banned) */
@Getter
@Setter
@NoArgsConstructor
public class DicBanned extends BaseEntity {

    private Long id;
    private String word;
    private String blockType = "BLOCK";         // BLOCK / MASK
    private boolean enabled = true;
    private String memo;
}

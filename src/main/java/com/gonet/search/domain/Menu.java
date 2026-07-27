package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사이트 메뉴 (검색 대상 원본, tn_menu) */
@Getter
@Setter
@NoArgsConstructor
public class Menu extends BaseEntity {

    private Long id;
    private String menuName;
    private String menuPath;                    // 이동 URL
    private String description;                 // 메뉴 설명 (색인 보조)
    private String useYn = "Y";
}

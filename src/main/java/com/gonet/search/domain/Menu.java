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

/** 사이트 메뉴 (검색 대상 원본) */
@Entity
@Table(name = "tn_menu")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_name", nullable = false, length = 200)
    private String menuName;

    @Column(name = "menu_path", nullable = false, length = 300)
    private String menuPath;                    // 이동 URL

    @Column(length = 500)
    private String description;                 // 메뉴 설명 (색인 보조)

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";
}

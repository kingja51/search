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

/** 동의어사전 — 같은 group_id = 서로 동의어 */
@Entity
@Table(name = "tn_search_dic_synonym")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DicSynonym extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(name = "is_representative", nullable = false)
    private boolean representative = false;     // 대표어 여부

    @Column(nullable = false)
    private boolean enabled = true;
}

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

/** 금지어사전 — BLOCK(검색차단) / MASK(결과숨김) */
@Entity
@Table(name = "tn_search_dic_banned")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DicBanned extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(name = "block_type", nullable = false, length = 20)
    private String blockType = "BLOCK";         // BLOCK / MASK

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 300)
    private String memo;
}

package com.gonet.search.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 집계 1행 (탭별 건수 / 카테고리별 건수) */
@Getter
@Setter
@NoArgsConstructor
public class KeyCount {

    private String key;                         // doc_type 또는 category 값
    private long count;
}

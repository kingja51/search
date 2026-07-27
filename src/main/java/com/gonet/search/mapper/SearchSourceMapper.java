package com.gonet.search.mapper;

import com.gonet.search.domain.SearchSource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SearchSourceMapper {

    /** 신규·변경 대상: 색인에 없거나 content_hash가 다른 문서 (해시 diff) */
    List<SearchSource> findChanged();

    /** 전체 소스 (전체 재색인용 — 해시 비교 없이 전량) */
    List<SearchSource> findAll();
}

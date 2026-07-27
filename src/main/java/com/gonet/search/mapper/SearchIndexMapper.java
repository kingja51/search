package com.gonet.search.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SearchIndexMapper {

    /** 색인 문서 수 (docType=null이면 전체) — index.documents 게이지용 */
    long count(@Param("docType") String docType);
}

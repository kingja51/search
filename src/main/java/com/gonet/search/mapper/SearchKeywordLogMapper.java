package com.gonet.search.mapper;

import com.gonet.search.domain.SearchKeywordLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SearchKeywordLogMapper {

    /** 검색 로그 적재 (감사 필드는 AuditInterceptor가 주입) */
    int insert(SearchKeywordLog log);
}

package com.gonet.search.mapper;

import com.gonet.search.domain.SearchIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchIndexMapper {

    /** 색인 문서 수 (docType=null이면 전체) — index.documents 게이지용 */
    long count(@Param("docType") String docType);

    /** 색인 배치 upsert — 감사 필드는 AuditInterceptor가 각 항목에 주입 (DESIGN.md 4.4 - 2단계) */
    int upsertBatch(@Param("items") List<SearchIndex> items);

    /** VIEW에서 사라진 문서(status=DELETED 등) 색인 제거 (DESIGN.md 4.4 - 3단계) */
    int deleteOrphans();
}

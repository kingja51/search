package com.gonet.search.mapper;

import com.gonet.search.domain.SearchIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchIndexMapper {

    /** 색인 문서 수 (docType=null이면 전체) — index.documents 게이지용 */
    long count(@Param("docType") String docType);

    /** 도메인·카테고리별 색인 문서 분포 (통계 화면) */
    List<com.gonet.search.dto.KeyCount> countByCategory();

    /** 색인 배치 upsert — 감사 필드는 AuditInterceptor가 각 항목에 주입 (DESIGN.md 4.4 - 2단계) */
    int upsertBatch(@Param("items") List<SearchIndex> items);

    /** VIEW에서 사라진 문서(status=DELETED 등) 색인 제거 (DESIGN.md 4.4 - 3단계) */
    int deleteOrphans();

    /** 파일 추출 결과를 색인에 직접 반영 (update-origin=false 기본 경로 — 마스킹 완료본) */
    int updateFileContent(@Param("docId") Long docId,
                          @Param("summary") String summary,
                          @Param("tokens") String tokens,
                          @Param("ip") String ip);
}
